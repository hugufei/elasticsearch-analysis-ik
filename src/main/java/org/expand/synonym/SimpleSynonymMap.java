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

import java.util.*;


public class SimpleSynonymMap {

    private static final Logger LOGGER = ESLoggerFactory.getLogger(SimpleSynonymMap.class);

    private Map<String, List<String>> sameRuleMap = new HashMap<String, List<String>>();// 完全对等的同义词map
    private Map<String, List<String>> rewriteRuleMap = new HashMap<String, List<String>>();// 改写的同义词map

    public SimpleSynonymMap() {
    }

    public void addRule(String rule) {
        try {
            String sides[] = split(rule, "=>");
            // 处理改写,比如配置的是 a==> b,c,d ,那么输入的是a，得到的是 b,c,d
            if (sides.length > 1) { // explicit mapping
                if (sides.length != 2) {
                    throw new IllegalArgumentException("more than one explicit mapping specified on the same line");
                }

                Set<String> inputList = new HashSet<>();
                String inputStrings[] = split(sides[0], ",");
                for (int i = 0; i < inputStrings.length; i++) {
                    inputList.add(process(inputStrings[i]));
                }

                Set<String> outputList = new HashSet<>();
                String outputStrings[] = split(sides[1], ",");
                for (int i = 0; i < outputStrings.length; i++) {
                    outputList.add(process(outputStrings[i]));
                }
                // these mappings are explicit and never preserve original
                for (String input : inputList) {
                    for (String output : outputList) {
                        addToRuleMap(input, output, true);
                    }
                }
            } else {
                //处理对等 配置a,b,c，则
                // 1) a 的同义词为 a,b，c
                // 2) b 的同义词为 a,b，c
                // 3) c 的同义词为 a,b，c
                List<String> inputList = new ArrayList<>();
                String inputStrings[] = split(rule, ",");
                for (int i = 0; i < inputStrings.length; i++) {
                    inputList.add(process(inputStrings[i]));
                }
                for (String input : inputList) {
                    for (String output : inputList) {
                        addToRuleMap(input, output, false);
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Add synonym rule failed. rule: " + rule, t);
        }
    }


    private void addToRuleMap(String inputString, String outputString, boolean isRewrite) {
        Map<String, List<String>> ruleMap = isRewrite ? this.rewriteRuleMap : this.sameRuleMap;
        List<String> outputs = ruleMap.get(inputString);
        if (outputs == null) {
            outputs = new ArrayList<>();
            ruleMap.put(inputString, outputs);
        }
        if (!outputs.contains(outputString)) {
            outputs.add(outputString);
        }
    }

    private static String[] split(String s, String separator) {
        List<String> list = new ArrayList<>(2);
        StringBuilder sb = new StringBuilder();
        int pos = 0, end = s.length();
        while (pos < end) {
            if (s.startsWith(separator, pos)) {
                if (sb.length() > 0) {
                    list.add(sb.toString());
                    sb = new StringBuilder();
                }
                pos += separator.length();
                continue;
            }
            char ch = s.charAt(pos++);
            if (ch == '\\') {
                sb.append(ch);
                if (pos >= end)
                    break; // ERROR, or let it go?
                ch = s.charAt(pos++);
            }
            sb.append(ch);
        }
        if (sb.length() > 0) {
            list.add(sb.toString());
        }
        return list.toArray(new String[list.size()]);
    }

    //处理斜杠和大小写
    private String process(String input) {
        String inputStr = input.trim().toLowerCase(Locale.getDefault());
        if (inputStr.indexOf("\\") >= 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < inputStr.length(); i++) {
                char ch = inputStr.charAt(i);
                if (ch == '\\' && i < inputStr.length() - 1) {
                    sb.append(inputStr.charAt(++i));
                } else {
                    sb.append(ch);
                }
            }
            return sb.toString();
        }
        return inputStr;
    }

    public List<String> getSynonymWords(String input, boolean isRewrite, boolean ignoreCase) {
        Map<String, List<String>> ruleMap = isRewrite ? this.rewriteRuleMap : this.sameRuleMap;
        String inputVar = ignoreCase ? input.trim().toLowerCase() : input;
        if (!ruleMap.containsKey(inputVar)) {
            return null;
        }
        return ruleMap.get(input);
    }

}
