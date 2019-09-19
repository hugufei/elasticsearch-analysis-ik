/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.expand.synonym;

import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.env.Environment;
import org.expand.HostUtils;
import org.expand.JDBCUtils;
import org.expand.dict.DictConfigurationResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SynonymRuleManager {

    private static final Logger LOGGER = ESLoggerFactory.getLogger(SynonymRuleManager.class);

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "monitor-thread");
        }
    });

    private static SynonymRuleManager singleton;

    private SimpleSynonymMap synonymMap;

    private static String getDbUrl(Environment env) {
        return DictConfigurationResolver.getDictConfiguration(env).getDbUrl();
    }

    public static synchronized SynonymRuleManager initial(Environment env) {
        if (singleton == null) {
            synchronized (SynonymRuleManager.class) {
                if (singleton == null) {
                    singleton = new SynonymRuleManager();
                    String dbUrl = getDbUrl(env);
                    long loadedMaxVersion = singleton.loadSynonymRule(dbUrl);
                    executorService.scheduleWithFixedDelay(new SynonymMonitor(dbUrl, loadedMaxVersion), 120, 120, TimeUnit.SECONDS);
                }
            }
        }
        return singleton;
    }


    public static SynonymRuleManager getSingleton() {
        if (singleton == null) {
            throw new IllegalStateException("Please initial first.");
        }
        return singleton;
    }

    public List<String> getSynonymWords(String inputToken, boolean isRewrite, boolean ignoreCase, Analyzer analyzer) {
        if (this.synonymMap == null) {
            return null;
        }
        List<String> synonymWords = this.synonymMap.getSynonymWords(inputToken, isRewrite, ignoreCase);
        if (synonymWords == null || synonymWords.isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (String synonymWord : synonymWords) {
            result.addAll(analyze(synonymWord, analyzer));
        }
        return result;
    }

    private static Set<String> analyze(String text, Analyzer analyzer) {
        Set<String> result = new HashSet<String>();
        TokenStream ts = null;
        try {
            ts = analyzer.tokenStream("", text);
            CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
            PositionIncrementAttribute posIncAtt = ts.addAttribute(PositionIncrementAttribute.class);
            ts.reset();
            while (ts.incrementToken()) {
                int length = termAtt.length();
                if (length == 0) {
                    throw new IllegalArgumentException("term: " + text + " analyzed to a zero-length token");
                }
                if (posIncAtt.getPositionIncrement() != 1) {
                    throw new IllegalArgumentException("term: " + text + " analyzed to a token with posinc != 1");
                }
                result.add(new String(termAtt.buffer(), 0, termAtt.length()));
            }
        } catch (Exception e) {
            LOGGER.error("text {} happen error:", text);
            LOGGER.error(e.getMessage(), e);
        } finally {
            try {
                ts.end();
                ts.close();
            } catch (Exception e2) {
                LOGGER.error(e2.getMessage(), e2);
            }
        }
        return result;
    }

    public static void main(String[] args) {
        WhitespaceAnalyzer analyzer = new WhitespaceAnalyzer();
        System.out.println(analyze("AB-C1", analyzer));
        System.out.println(analyze("AB-C2", analyzer));
        System.out.println(analyze("AB-C3", analyzer));
        System.out.println(analyze("AB-C4", analyzer));
    }


    private long loadSynonymRule(String dbUrl) {
        try {
            long currentMaxVersion = JDBCUtils.queryMaxSynonymRuleVersion(dbUrl);
            this.synonymMap = this.loadSynonymFromDB(dbUrl, currentMaxVersion);
            return currentMaxVersion;
        } catch (Exception e) {
            LOGGER.error("Load synonym rule failed!", e);
            return 0L;
        }
    }

    public boolean reloadSynonymRule(String dbUrl, long maxVersion) {
        LOGGER.info("Start to reload synonym rule...");
        try {
            SimpleSynonymMap tempSynonymMap = loadSynonymFromDB(dbUrl, maxVersion);
            this.synonymMap = tempSynonymMap;
            return true;
        } catch (Throwable t) {
            LOGGER.error("Failed to reload synonym rule!", t);
            return false;
        }
    }

    private SimpleSynonymMap loadSynonymFromDB(String dbUrl, long maxVersion) throws Exception {
        SimpleSynonymMap tempSynonymMap = new SimpleSynonymMap();
        if (maxVersion <= 0) {
            return tempSynonymMap;
        }
        List<String> synonymRuleList = JDBCUtils.querySynonymRules(dbUrl, maxVersion);
        if (synonymRuleList == null || synonymRuleList.isEmpty()) {
            JDBCUtils.recordLog(dbUrl, String.format("load synonym failed, ip=%s, maxVersion=%d", HostUtils.getIp(), maxVersion));
            return tempSynonymMap;
        }
        for (String rule : synonymRuleList) {
            tempSynonymMap.addRule(rule);
        }
        LOGGER.info("load synonym rule succeed, count={}, maxVersion={}", synonymRuleList.size(), maxVersion);
        JDBCUtils.recordLog(dbUrl, String.format("load synonym succeed, ip= %s, count=%d, maxVersion=%d", HostUtils.getIp(), synonymRuleList.size(), maxVersion));
        return tempSynonymMap;
    }
}
