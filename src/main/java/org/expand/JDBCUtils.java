package org.expand;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.expand.dict.WordType;
import org.wltea.analyzer.dic.Monitor;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简易的数据库操作工具类。
 */
public final class JDBCUtils {

    private static final String AES_KEY = "1qaz2wsx3edc4rfv";

    private static final Logger logger = ESLoggerFactory.getLogger(Monitor.class.getName());

    public static Map<WordType, Long> queryMaxVersion(String dbUrl) throws Exception {
        Map<WordType, Long> maxVersionMap = new HashMap<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        WordType wordType;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(dbUrl);
            stmt = conn.createStatement();
            String sql = "SELECT word_type WORD_TYPE, max(last_update_time) VERSION FROM es_word_def GROUP BY word_type";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                if ((wordType = WordType.fromCode(rs.getInt("WORD_TYPE"))) != null) {
                    maxVersionMap.put(wordType, rs.getLong("VERSION"));
                }
            }
        } finally {
            closeQuietly(conn, stmt, rs);
        }

        return maxVersionMap;
    }

    public static List<String> getDynWords(String dictDBUrl, WordType wordType, long maxVersion) {
        List<String> list = new ArrayList<String>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(dictDBUrl);
            stmt = conn.createStatement();
            String sql = "select word from es_word_def where last_update_time <= " + maxVersion
                    + " and word_type = " + wordType.getCode() + " and status = 1";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                list.add(rs.getString("word"));
            }
        } catch (ClassNotFoundException e) {
            logger.error("Load words from DB failed.", e);
        } catch (SQLException e) {
            logger.error("Load words from DB failed.", e);
        } finally {
            closeQuietly(conn, stmt, rs);
        }

        return list;
    }

    public static void recordLog(String dictDBUrl, String logContent) {
        Connection conn = null;
        Statement stmt = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(dictDBUrl);
            stmt = conn.createStatement();
            String sql = "insert es_word_log(log_content) values('" + logContent + "')";
            stmt.execute(sql);
        } catch (ClassNotFoundException e) {
            logger.error("Record log to DB failed. logContent=" + logContent, e);
        } catch (SQLException e) {
            logger.error("Record log to DB failed. logContent=" + logContent, e);
        } finally {
            closeQuietly(conn, stmt, null);
        }
    }


    private static void closeQuietly(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }
    }

    public static String processEncryptPassword(String sourceDBUrl) {
        if (sourceDBUrl == null || sourceDBUrl.trim().isEmpty()) {
            return sourceDBUrl;
        }

        //jdbc:mysql://192.168.102.216:3306/yhb_search_dev?user=yh_test&password=9nm0icOwt6bMHjMusIfMLw==&useUnicode=true&characterEncoding=UTF8
        Pattern pattern = Pattern.compile(".*&password=(.*)&useUnicode.*");
        Matcher matcher = pattern.matcher(sourceDBUrl);
        if (matcher.find()) {
            String encryptPwd = matcher.group(1);
            try {
                String pwd = decryptPassword(encryptPwd);
                return sourceDBUrl.replace(encryptPwd, pwd);
            } catch (Exception e) {
                throw new RuntimeException("Failed to decrypt password!", e);
            }
        }

        return sourceDBUrl;
    }

    private static String decryptPassword(String passwordStr)
            throws NoSuchAlgorithmException,
            NoSuchPaddingException,
            InvalidKeyException,
            InvalidAlgorithmParameterException,
            IllegalBlockSizeException,
            BadPaddingException, IOException, ClassNotFoundException {

        byte[] keyBytes = AES_KEY.getBytes();
        byte[] dataBytes = Base64.decodeBase64(passwordStr);

        //必须为 128bit或256bit,即 16字节或32字节
        if (keyBytes.length != 16 && keyBytes.length != 32) {
            return "";
        }

        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec k = new SecretKeySpec(keyBytes, "AES");
        cipher.init(Cipher.DECRYPT_MODE, k);
        return new String(cipher.doFinal(dataBytes));
    }

    public static void main(String[] args) {
        String dictDBUrl = "jdbc:mysql://192.168.102.216:3306/yhb_search_2016?user=yh_test&password=9nm0icOwt6bMHjMusIfMLw==&useUnicode=true&characterEncoding=UTF8";
        System.out.println(processEncryptPassword(dictDBUrl));
    }
}
