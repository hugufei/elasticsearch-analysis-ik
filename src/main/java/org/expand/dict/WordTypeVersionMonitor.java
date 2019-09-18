package org.expand.dict;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.expand.JDBCUtils;
import org.wltea.analyzer.dic.Dictionary;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库词库类型监视器
 */
public class WordTypeVersionMonitor implements Runnable {

    private static final Logger logger = ESLoggerFactory.getLogger(WordTypeVersionMonitor.class.getName());

    private String dicDbUrl;
    private Map<WordType, Long> versionMap = new HashMap<>();

    public WordTypeVersionMonitor(String dicDbUrl) {
        this.dicDbUrl = dicDbUrl;
        this.versionMap = getVersionMapFromDB();
    }

    public Map<WordType, Long> getVersionMap() {
        return Collections.unmodifiableMap(this.versionMap);
    }

    @Override
    public void run() {
        try {
            Map<WordType, Long> updateVersionMap = this.checkModify();
            if (updateVersionMap != null && !updateVersionMap.isEmpty()) {
                //按需重新加载词库
                Dictionary.getSingleton().reloadDBDict(dicDbUrl, updateVersionMap);
                //更新版本信息
                this.versionMap.putAll(updateVersionMap);
            }
        } catch (Exception e) {
            logger.error("Failed to reload db dict!", e);
        }
    }

    private Map<WordType, Long> getVersionMapFromDB() {
        try {
            Map<WordType, Long> tempVersionMap = JDBCUtils.queryMaxVersion(dicDbUrl);
            for (WordType wordType : WordType.values()) {
                tempVersionMap.computeIfAbsent(wordType, key -> 0L);
            }
            return tempVersionMap;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private Map<WordType, Long> checkModify() throws Exception {
        Map<WordType, Long> updateVersionMap = new HashMap<>();
        Map<WordType, Long> tempVersionMap = getVersionMapFromDB();
        for (Map.Entry<WordType, Long> entry : tempVersionMap.entrySet()) {
            if (!entry.getValue().equals(this.versionMap.getOrDefault(entry.getKey(), 0L))) {
                updateVersionMap.put(entry.getKey(), entry.getValue());
            }
        }
        return updateVersionMap;
    }


}