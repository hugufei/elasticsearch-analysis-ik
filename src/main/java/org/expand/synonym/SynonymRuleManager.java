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
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.env.Environment;
import org.expand.dict.DictConfigurationResolver;
import org.expand.HostUtils;
import org.expand.JDBCUtils;
import org.wltea.analyzer.dic.Monitor;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Created by ginozhang on 2017/1/12.
 */
public class SynonymRuleManager {

	private static final Logger LOGGER = ESLoggerFactory.getLogger(Monitor.class.getName());

	private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1, new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, "monitor-thread");
		}
	});

	private static SynonymRuleManager singleton;

	private SynonymConfiguration configuration;

	private SimpleSynonymMap synonymMap;
	
	private static String getDbUrl(Environment env){
		return DictConfigurationResolver.getDictConfiguration(env).getDbUrl();
	}
	
	public static synchronized SynonymRuleManager initial(SynonymConfiguration cfg, Environment env) {
		if (singleton == null) {
			synchronized (SynonymRuleManager.class) {
				if (singleton == null) {
					singleton = new SynonymRuleManager();
					singleton.configuration = cfg;
					long loadedMaxVersion = singleton.loadSynonymRule(getDbUrl(env));
					executorService.scheduleWithFixedDelay(new SynonymMonitor(getDbUrl(env), loadedMaxVersion), 120, 120, TimeUnit.SECONDS);
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

	public List<String> getSynonymWords(String inputToken, boolean isRewrite) {
		if (this.synonymMap == null) {
			return null;
		}
		return this.synonymMap.getSynonymWords(inputToken, isRewrite);
	}

	private long loadSynonymRule(String dbUrl) {
		try {
			long currentMaxVersion = JDBCUtils.queryMaxSynonymRuleVersion(dbUrl);
			this.synonymMap = this.loadSynonymFromDB(this.configuration, dbUrl,currentMaxVersion);
			return currentMaxVersion;
		} catch (Exception e) {
			LOGGER.error("Load synonym rule failed!", e);
			return 0L;
		}
	}

	public boolean reloadSynonymRule(String dbUrl, long maxVersion) {
		LOGGER.info("Start to reload synonym rule...");
		try {
			SynonymRuleManager tmpManager = new SynonymRuleManager();
			tmpManager.configuration = getSingleton().configuration;
			SimpleSynonymMap tempSynonymMap = tmpManager.loadSynonymFromDB(tmpManager.configuration, dbUrl, maxVersion);
			this.synonymMap = tempSynonymMap;
			return true;
		} catch (Throwable t) {
			LOGGER.error("Failed to reload synonym rule!", t);
			return false;
		}
	}

	private SimpleSynonymMap loadSynonymFromDB(SynonymConfiguration cfg, String dbUrl, long maxVersion) throws Exception {
		SimpleSynonymMap tempSynonymMap = new SimpleSynonymMap(cfg);
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
		JDBCUtils.recordLog(dbUrl,
				String.format("load synonym succeed, ip= %s, count=%d, maxVersion=%d", HostUtils.getIp(), synonymRuleList.size(), maxVersion));
		return tempSynonymMap;
	}
}
