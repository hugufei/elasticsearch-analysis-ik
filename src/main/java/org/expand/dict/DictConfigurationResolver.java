package org.expand.dict;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;

import java.io.*;
import java.nio.file.Path;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;


/**
 * 从/config/IKAnalyzer.cfg.xml中解析配置类
 *
 * 同义词和主词库会使用同一份数据库配置
 */
public class DictConfigurationResolver {

    private static final Logger logger = ESLoggerFactory.getLogger(DictConfigurationResolver.class);

    private final static String FILE_NAME = "IKAnalyzer.cfg.xml";

    private static Path getConfigInPluginDir() {
        return PathUtils.get(new File(AnalysisIkPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent(), "config")
                .toAbsolutePath();
    }

    private static volatile DictConfiguration dictConfiguration;

    public static DictConfiguration getDictConfiguration(Environment env) {
        if (dictConfiguration == null) {
            synchronized (DictConfigurationResolver.class) {
                if (dictConfiguration == null) {
                    // 记录词典配置
                    dictConfiguration = buildDictConfiguration(env);
                    logger.info("dbUrl is : " + dictConfiguration.getDbUrl());
                }
            }
        }
        return dictConfiguration;
    }

    private static DictConfiguration buildDictConfiguration(Environment env) {
        Properties props = new Properties();
        Path conf_dir = env.configFile().resolve(AnalysisIkPlugin.PLUGIN_NAME);
        Path configFile = conf_dir.resolve(FILE_NAME);

        InputStream input = null;
        try {
            logger.info("try load config from {}", configFile);
            input = new FileInputStream(configFile.toFile());
        } catch (FileNotFoundException e) {
            conf_dir = getConfigInPluginDir();
            configFile = conf_dir.resolve(FILE_NAME);
            try {
                logger.info("try load config from {}", configFile);
                input = new FileInputStream(configFile.toFile());
            } catch (FileNotFoundException ex) {
                // We should report origin exception
                logger.error("ik-analyzer", e);
            }
        }
        if (input != null) {
            try {
                props.loadFromXML(input);
            } catch (InvalidPropertiesFormatException e) {
                logger.error("ik-analyzer", e);
            } catch (IOException e) {
                logger.error("ik-analyzer", e);
            }
        }
        return createDictConfiguration(props);
    }

    private static DictConfiguration createDictConfiguration(Properties props) {
        DictConfiguration dictConfiguration = new DictConfiguration();
        dictConfiguration.setDict_db_load_only(getProperty(props, "dict_db_load_only"));
        dictConfiguration.setDict_db_server(getProperty(props, "dict_db_server"));
        dictConfiguration.setDict_db_port(getProperty(props, "dict_db_port"));
        dictConfiguration.setDict_db_database(getProperty(props, "dict_db_database"));
        dictConfiguration.setDict_db_user(getProperty(props, "dict_db_user"));
        dictConfiguration.setDict_db_password(getProperty(props, "dict_db_password"));
        dictConfiguration.setDict_db_check_interval(getProperty(props, "dict_db_check_interval"));
        return dictConfiguration;
    }

    private static String getProperty(Properties props, String key) {
        if (props != null) {
            return props.getProperty(key);
        }
        return null;
    }

}
