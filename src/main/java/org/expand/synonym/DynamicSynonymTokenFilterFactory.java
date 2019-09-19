package org.expand.synonym;

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

import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.dic.Monitor;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class DynamicSynonymTokenFilterFactory extends AbstractTokenFilterFactory {

    private static final Logger logger = ESLoggerFactory.getLogger(Monitor.class.getName());

    private boolean isRewrite;
    private boolean ignoreCase;
    private Analyzer analyzer;
    private boolean expand;

    public DynamicSynonymTokenFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings, boolean isRewrite) {
        super(indexSettings, name, settings);

        // 获取同义词过滤器的配置项
        this.isRewrite = isRewrite;
        this.ignoreCase = settings.getAsBoolean("synonym_ignore_case", true);
        this.expand = settings.getAsBoolean("synonym_expand", true);
        String analyzerName = settings.get("synonym_analyzer", "whitespace");
        this.analyzer = buildAnalyzer(analyzerName, env, settings);

        logger.info("indexname is [{}] , isRewrite is [{}], ignoreCase is [{}] ,expand is [{}],analyzerName is[{}] ",indexSettings.getIndex().getName(),isRewrite,ignoreCase,expand,analyzerName);

        // 初始化同义词词库
        SynonymRuleManager.initial(env);
    }

    public static DynamicSynonymTokenFilterFactory getDynamicSameSynonymTokenFilter(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        return new DynamicSynonymTokenFilterFactory(indexSettings, env, name, settings, false);
    }

    public static DynamicSynonymTokenFilterFactory getDynamicRewriteSynonymTokenFilter(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        return new DynamicSynonymTokenFilterFactory(indexSettings, env, name, settings, true);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new DynamicSynonymTokenFilter(tokenStream, isRewrite, ignoreCase, analyzer);
    }

    private Analyzer buildAnalyzer(String analyzerName, Environment env, Settings settings) {
        if ("standand".equalsIgnoreCase(analyzerName)) {
            return new StandardAnalyzer();
        } else if ("keyword".equalsIgnoreCase(analyzerName)) {
            return new KeywordAnalyzer();
        } else if ("simple".equalsIgnoreCase(analyzerName)) {
            return new SimpleAnalyzer();
        } else if ("ik_max_word".equalsIgnoreCase(analyzerName)) {
            Configuration configuration = new Configuration(env, settings).setUseSmart(false);
            return new IKAnalyzer(configuration);
        } else if ("ik_smart".equalsIgnoreCase(analyzerName)) {
            Configuration configuration = new Configuration(env, settings).setUseSmart(true);
            return new IKAnalyzer(configuration);
        }
        return new WhitespaceAnalyzer();
    }
}
