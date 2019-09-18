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
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.lucene.IKAnalyzer;

/**
 * 同义词配置
 */
public class SynonymConfiguration {

	private static final Logger LOGGER = ESLoggerFactory.getLogger(SynonymConfiguration.class);

	//是否区分大小写，默认为true
	private final boolean ignoreCase;

	private final boolean expand;

	private final Analyzer analyzer;

	private SynonymConfiguration(boolean ignoreCase, boolean expand, Analyzer analyzer) {
		this.ignoreCase = ignoreCase;
		this.expand = expand;
		this.analyzer = analyzer;
	}

	public static SynonymConfiguration createSynonymConfiguration(IndexSettings indexSettings, Environment env, String name, Settings settings) {
		// get the filter setting params
		final boolean ignoreCase = settings.getAsBoolean("synonym_ignore_case", false);
		final boolean expand = settings.getAsBoolean("synonym_expand", true);
		final String analyzerName = settings.get("synonym_analyzer", "whitespace");

		LOGGER.info("begin createSynonymConfiguration, index name is [{}],ignoreCase is[{}],expand is[{}],analyzerName is[{}]", indexSettings.getIndex().getName(),ignoreCase,expand,analyzerName);

		Analyzer analyzer;
		if ("standand".equalsIgnoreCase(analyzerName)) {
			analyzer = new StandardAnalyzer();
		} else if ("keyword".equalsIgnoreCase(analyzerName)) {
			analyzer = new KeywordAnalyzer();
		} else if ("simple".equalsIgnoreCase(analyzerName)) {
			analyzer = new SimpleAnalyzer();
		} else if ("ik_max_word".equalsIgnoreCase(analyzerName)) {
			Configuration configuration = new Configuration(env, settings).setUseSmart(false);
			analyzer = new IKAnalyzer(configuration);
		} else if ("ik_smart".equalsIgnoreCase(analyzerName)) {
			Configuration configuration = new Configuration(env, settings).setUseSmart(true);
			analyzer = new IKAnalyzer(configuration);
		} else {
			analyzer = new WhitespaceAnalyzer();
		}
		return new SynonymConfiguration(ignoreCase, expand, analyzer);
	}

	public Analyzer getAnalyzer() {
		return analyzer;
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}

	public boolean isExpand() {
		return expand;
	}
	
}
