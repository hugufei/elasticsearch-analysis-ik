package org.expand.dict;

import org.expand.JDBCUtils;

/**
 * 数据库词库配置定义类
 *
 */
public class DictConfiguration {

	// 是否只从数据库中加载词库
	private String dict_db_load_only;

	// 数据库ip
	private String dict_db_server;

	// 数据库port
	private String dict_db_port;

	// 数据库名称
	private String dict_db_database;

	// 数据库用户
	private String dict_db_user;

	// 数据库密码
	private String dict_db_password;

	// 检查数据库的时间间隔
	private String dict_db_check_interval;

	public void setDict_db_load_only(String dict_db_load_only) {
		this.dict_db_load_only = dict_db_load_only;
	}

	public void setDict_db_server(String dict_db_server) {
		this.dict_db_server = dict_db_server;
	}

	public void setDict_db_port(String dict_db_port) {
		this.dict_db_port = dict_db_port;
	}

	public void setDict_db_database(String dict_db_database) {
		this.dict_db_database = dict_db_database;
	}

	public void setDict_db_user(String dict_db_user) {
		this.dict_db_user = dict_db_user;
	}

	public void setDict_db_password(String dict_db_password) {
		this.dict_db_password = dict_db_password;
	}

	public void setDict_db_check_interval(String dict_db_check_interval) {
		this.dict_db_check_interval = dict_db_check_interval;
	}
	
	public String getDbUrl() {
		try {
			if (!"true".equalsIgnoreCase(this.dict_db_load_only)) {
				return null;
			}
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("jdbc:mysql://").append(this.dict_db_server).append(":").append(this.dict_db_port).append("/").append(this.dict_db_database);
			stringBuilder.append("?user=").append(this.dict_db_user);
			stringBuilder.append("&password=").append(this.dict_db_password);
			stringBuilder.append("&useUnicode=true");
			stringBuilder.append("&characterEncoding=UTF8");
			return JDBCUtils.processEncryptPassword(stringBuilder.toString());
		} catch (Exception e) {
			return null;
		}
	}
	
	public int getDict_db_check_interval() {
		try {
			return Integer.parseInt(dict_db_check_interval);
		} catch (Exception e) {
			return 120;
		}
	}

	@Override
	public String toString() {
		return "DictConfiguration [dict_db_load_only=" + dict_db_load_only + ", dict_db_server=" + dict_db_server + ", dict_db_port=" + dict_db_port
				+ ", dict_db_database=" + dict_db_database + ", dict_db_user=" + dict_db_user + ", dict_db_password=" + dict_db_password
				+ ", dict_db_check_interval=" + dict_db_check_interval + "]";
	}
	
}
