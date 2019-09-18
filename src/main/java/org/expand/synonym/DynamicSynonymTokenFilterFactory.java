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

import org.apache.lucene.analysis.TokenStream;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

public class DynamicSynonymTokenFilterFactory extends AbstractTokenFilterFactory {

	private boolean isRewrite;

	public DynamicSynonymTokenFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings, boolean isRewrite) {
		super(indexSettings, name, settings);
		// 获取自定义同义词配置
		SynonymConfiguration synonymConfiguration = SynonymConfiguration.createSynonymConfiguration(indexSettings, env, name, settings);
		// 初始化同义词
		SynonymRuleManager.initial(synonymConfiguration,env);
		this.isRewrite = isRewrite;
	}

	public static DynamicSynonymTokenFilterFactory getDynamicSameSynonymTokenFilter(IndexSettings indexSettings, Environment env, String name, Settings settings) {
		return new DynamicSynonymTokenFilterFactory(indexSettings, env, name, settings, false);
	}
	
	public static DynamicSynonymTokenFilterFactory getDynamicRewriteSynonymTokenFilter(IndexSettings indexSettings, Environment env, String name, Settings settings) {
		return new DynamicSynonymTokenFilterFactory(indexSettings, env, name, settings, true);
	}

	@Override
	public TokenStream create(TokenStream tokenStream) {
		return new DynamicSynonymTokenFilter(tokenStream, isRewrite);
	}
}
