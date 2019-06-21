package top.microiot.api;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * http 会话配置属性类
 *
 * @author 曹新宇
 */
@ConfigurationProperties(prefix = "microiot.connect")
public class HttpSessionProperties {
	private String username;
	private String password;
	private String uri;
	
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
}