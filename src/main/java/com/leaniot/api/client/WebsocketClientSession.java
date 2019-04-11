package com.leaniot.api.client;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.stomp.WebSocketStompSessionManager;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import com.leaniot.api.client.stomp.AlarmSubscriber;
import com.leaniot.api.client.stomp.RequestAction;
import com.leaniot.api.client.stomp.RequestGet;
import com.leaniot.api.client.stomp.RequestSet;
import com.leaniot.api.client.stomp.SubscribeAlarm;
import com.leaniot.api.dto.Response;
import com.leaniot.domain.ActionType;
import com.leaniot.domain.Device;
import com.leaniot.domain.attribute.AttValueInfo;
import com.leaniot.domain.attribute.AttributeType;
import com.leaniot.domain.attribute.DataType;
import com.leaniot.domain.attribute.Location;
import com.leaniot.exception.NotFoundException;
import com.leaniot.exception.StatusException;
import com.leaniot.exception.ValueException;

/**
 * 客户端与物联网平台的websocket会话
 *
 * @author 曹新宇
 */
public class WebsocketClientSession  extends WebSocketStompSessionManager {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private HttpClientSession session;
	private long timeout;
	
	public HttpClientSession getSession() {
		return session;
	}

	/**
	 * 客户端与物联网平台websocket会话构造函数。
	 * @param session 客户端http会话。
	 * @param webSocketStompClient 客户端与物联网平台websocket底层连接。
	 * @param timeout 客户端与物联网平台websocket响应超时时长，单位为秒。
	 */
	public WebsocketClientSession(HttpClientSession session, WebSocketStompClient webSocketStompClient, long timeout) {
		super(webSocketStompClient, session.getWSUri());
		this.session = session;
		this.timeout = timeout;
	}

	/**
	 * 客户端设置收到告警后的告警处理。
	 * @param deviceId 被监控告警的设备。
	 * @param subscriber 收到告警后的处理。
	 * @see com.leaniot.api.client.stomp.AlarmSubscriber
	 * @return 返回告警处理。
	 */
	public SubscribeAlarm subscribe(String deviceId, AlarmSubscriber subscriber) {
		subscriber.setWebsocketClientSession(this);
		SubscribeAlarm sessionHandler = new SubscribeAlarm(deviceId, subscriber);
        connect(sessionHandler);
        return sessionHandler;
	}
	
	/**
	 * 客户端get设备属性值。属性值类型为整型。
	 * @param deviceId 获取属性值的设备。
	 * @param attribute 属性名称。
	 * @return 返回整型属性值。
	 */
	public int getInt(String deviceId, String attribute) {
		int value = get(deviceId, attribute, Integer.class);
		return value;
	}
	
	/**
	 * 客户端get设备属性值。属性值类型为double。
	 * @param deviceId 获取属性值的设备。
	 * @param attribute 属性名称。
	 * @return 返回double属性值。
	 */
	public double getDouble(String deviceId, String attribute) {
		double value = get(deviceId, attribute, Double.class);
		return value;
	}
	
	/**
	 * 客户端get设备属性值。属性值类型为字符串。
	 * @param deviceId 获取属性值的设备。
	 * @param attribute 属性名称。
	 * @return 返回字符串属性值。
	 */
	public String getString(String deviceId, String attribute) {
		String value = get(deviceId, attribute, String.class);
		return value;
	}
	
	/**
	 * 客户端get设备属性值。属性值类型为日期。
	 * @param deviceId 获取属性值的设备。
	 * @param attribute 属性名称。
	 * @return 返回日期属性值。
	 */
	public Date getDate(String deviceId, String attribute) {
		Date value = get(deviceId, attribute, Date.class);
		return value;
	}
	
	/**
	 * 客户端get设备属性值。属性值类型为枚举。
	 * @param deviceId 获取属性值的设备。
	 * @param attribute 属性名称。
	 * @return 返回枚举属性值。
	 */
	public String getEnum(String deviceId, String attribute) {
		String value = get(deviceId, attribute, String.class);
		return value;
	}
	
	/**
	 * 客户端get设备属性值。属性值类型为location。
	 * @param deviceId 获取属性值的设备。
	 * @param attribute 属性名称。
	 * @return 返回location属性值。
	 */
	public Location getLocation(String deviceId, String attribute) {
		Location value = get(deviceId, attribute, Location.class);
		return value;
	}
	
	/**
	 * 客户端get设备属性值。属性值类型为bool。
	 * @param deviceId 获取属性值的设备。
	 * @param attribute 属性名称。
	 * @return 返回bool属性值。
	 */
	public boolean getBool(String deviceId, String attribute) {
		Boolean value = get(deviceId, attribute, Boolean.class);
		return value;
	}
	
	/**
	 * 客户端get设备属性值。
	 * @param deviceId 获取属性值的设备。
	 * @param attribute 属性名称。
	 * @param responseType 返回属性值的类型。
	 * @return 返回属性值。
	 */
	public <T> T get(String deviceId, String attribute, Class<T> responseType) {
		GetHandler<T> g = new GetHandler<T>(session, deviceId, attribute, responseType);
		return g.get();
	}

	public <T> T get(String deviceId, String attribute, ParameterizedTypeReference<T> responseType) {
		GetHandler<T> a = new GetHandler<T>(session, deviceId, attribute, responseType);
		return a.get();
	}
	/**
	 * 客户端set设备属性值。
	 * @param deviceId 设置属性值的设备。
	 * @param attribute 属性名称。
	 * @param value 属性值。
	 */
	public void set(String deviceId, String attribute, Object value) {
		Device device = session.getDevice(deviceId);
		if(device == null)
			throw new NotFoundException("device: " + deviceId);
		AttributeType attType = device.getDeviceType().getAttDefinition().get(attribute);
		if(attType == null)
			throw new NotFoundException("attribute: " + attribute);
		try{
			AttValueInfo attributeValue = attType.getAttValue(value);
			Response response = set(deviceId, attribute, attributeValue);
			if(!response.isSuccess())
				throw new StatusException(response.getError());
		} catch(Throwable e) {
			logger.error("set attribute [" + attribute + "] error: ", e);
			throw new ValueException("set attribute [" + attribute + "] error: " + e.getMessage());
		}
	}
	
	private Response set(String deviceId, String attribute, AttValueInfo value) {
		RequestSet request = new RequestSet(deviceId, attribute, value);
        
        connect(request);
		
        try {
			return request.get(timeout, TimeUnit.SECONDS);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new StatusException(e.getMessage());
		}
	}
	
	/**
	 * 客户端调用设备action操作。操作响应类型为整形
	 * @param deviceId 被调用的设备。
	 * @param action 操作名称。
	 * @param request 操作请求值。
	 * @return 返回整形类型操作响应。
	 */
	public int actionInt(String deviceId, String action, Object request) {
		int value = action(deviceId, action, request, Integer.class);
		return value;
	}
	
	/**
	 * 客户端调用设备action操作。操作响应类型为double
	 * @param deviceId 被调用的设备。
	 * @param action 操作名称。
	 * @param request 操作请求值。
	 * @return 返回double类型操作响应。
	 */
	public double actionDouble(String deviceId, String action, Object request) {
		double value = action(deviceId, action, request, Double.class);
		return value;
	}
	
	/**
	 * 客户端调用设备action操作。操作响应类型为字符串
	 * @param deviceId 被调用的设备。
	 * @param action 操作名称。
	 * @param request 操作请求值。
	 * @return 返回字符串类型操作响应。
	 */
	public String actionString(String deviceId, String action, Object request) {
		String value = action(deviceId, action, request, String.class);
		return value;
	}
	
	/**
	 * 客户端调用设备action操作。操作响应类型为日期。
	 * @param deviceId 被调用的设备。
	 * @param action 操作名称。
	 * @param request 操作请求值。
	 * @return 返回日期类型操作响应。
	 */
	public Date actionDate(String deviceId, String action, Object request) {
		Date value = action(deviceId, action, request, Date.class);
		return value;
	}
	
	/**
	 * 客户端调用设备action操作。操作响应类型为枚举。
	 * @param deviceId 被调用的设备。
	 * @param action 操作名称。
	 * @param request 操作请求值。
	 * @return 返回枚举类型操作响应。
	 */
	public String actionEnum(String deviceId, String action, Object request) {
		String value = action(deviceId, action, request, String.class);
		return value;
	}
	
	/**
	 * 客户端调用设备action操作。操作响应类型为location。
	 * @param deviceId 被调用的设备。
	 * @param action 操作名称。
	 * @param request 操作请求值。
	 * @return 返回location类型操作响应。
	 */
	public Location actionLocation(String deviceId, String action, Object request) {
		Location value =action(deviceId, action, request, Location.class);
		return value;
	}
	
	/**
	 * 客户端调用设备action操作。操作响应类型为bool。
	 * @param deviceId 被调用的设备。
	 * @param action 操作名称。
	 * @param request 操作请求值。
	 * @return 返回bool类型操作响应。
	 */
	public boolean actionBool(String deviceId, String action, Object request) {
		Boolean value = action(deviceId, action, request, Boolean.class);
		return value;
	}
	
	/**
	 * 客户端调用设备action操作。
	 * @param deviceId 被调用的设备。
	 * @param action 操作名称。
	 * @param request 操作请求值。
	 * @param responseType 返回响应值的类型。
	 * @return 返回操作响应。
	 */
	public <T> T action(String deviceId, String action, Object request, Class<T> responseType) {
		ActionHandler<T> a = new ActionHandler<T>(session, deviceId, action, request, responseType);
		return a.action();
	}
	
	public <T> T action(String deviceId, String action, Object request, ParameterizedTypeReference<T> responseType) {
		ActionHandler<T> a = new ActionHandler<T>(session, deviceId, action, request, responseType);
		return a.action();
	}
	
	@SuppressWarnings("rawtypes")
	public void action(String deviceId, String action, Object request) {
		ActionHandler a = new ActionHandler(session, deviceId, action, request);
		a.action();
	}
	
	public void stop() {
		destroy();
		session.stop();
	}

	private class GetHandler<T> {
		private HttpClientSession session;
		private String deviceId;
		private String attribute;
		private Response response;
		private DataType responseDataType;
		
		private Class<T> responseTypeClass = null;
		private ParameterizedTypeReference<T> responseType = null;
		
		public GetHandler(HttpClientSession session, String deviceId, String attribute, Class<T> responseTypeClass) {
			super();
			this.session = session;
			this.deviceId = deviceId;
			this.attribute = attribute;
			this.responseTypeClass = responseTypeClass;
		}
		
		public GetHandler(HttpClientSession session, String deviceId, String attribute, ParameterizedTypeReference<T> responseType) {
			super();
			this.session = session;
			this.deviceId = deviceId;
			this.attribute = attribute;
			this.responseType = responseType;
		}
		
		public T get() {
			Device device = session.getDevice(deviceId);
			if(device == null)
				throw new NotFoundException("device: " + deviceId);
			responseDataType = device.getDeviceType().getAttDefinition().get(attribute).getDataType();
			if(responseDataType == null)
				throw new NotFoundException("attribute: " + attribute);
			try {
				response = get(deviceId, attribute);
				if(!response.isSuccess())
					throw new StatusException(response.getError());
				else
					return getResponse();
			} catch(Throwable e) {
				logger.error("get attribute [" + attribute + "] error: ", e);
				throw new ValueException("get attribute [" + attribute + "] error: " + e.getMessage());
			}
		}
		
		@SuppressWarnings("unchecked")
		private T getResponse() {
			if(responseType == null)
				return (T) responseDataType.getData(response.getValue(), responseTypeClass);
			else
				return (T) responseDataType.getData(response.getValue(), responseType);
		}
		
		private Response get(String deviceId, String attribute) {
			RequestGet request = new RequestGet(deviceId, attribute);
	        
	        connect(request);
			
	        try {
				return request.get(timeout, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new StatusException(e.getMessage());
			}
		}
	}
	
	private class ActionHandler<T> {
		private HttpClientSession session;
		private String deviceId;
		private String action;
		private Object request;
		private Response response;
		private DataType responseDataType;
		
		private Class<T> responseTypeClass = null;
		private ParameterizedTypeReference<T> responseType = null;
		
		public ActionHandler(HttpClientSession session, String deviceId, String action, Object request) {
			super();
			this.session = session;
			this.deviceId = deviceId;
			this.action = action;
			this.request = request;
		}
		
		public ActionHandler(HttpClientSession session, String deviceId, String action, Object request, Class<T> responseTypeClass) {
			super();
			this.session = session;
			this.deviceId = deviceId;
			this.action = action;
			this.request = request;
			this.responseTypeClass = responseTypeClass;
		}

		public ActionHandler(HttpClientSession session, String deviceId, String action, Object request, ParameterizedTypeReference<T> responseType) {
			super();
			this.session = session;
			this.deviceId = deviceId;
			this.action = action;
			this.request = request;
			this.responseType = responseType;
		}
		
		public T action() {
			Device device = ((HttpClientSession) session).getDevice(deviceId);
			if(device == null)
				throw new NotFoundException("device: " + deviceId);
			ActionType actionType = device.getDeviceType().getActionTypes().get(action);
			if(actionType == null)
				throw new NotFoundException("action: " + action);
			AttValueInfo requestValue = null;
			if(actionType.getRequest() != null) {
				DataType requestType = actionType.getRequestAttributeType().getDataType();
				
				try{
					requestValue = requestType.getAttValue(request);
				} catch(Throwable e) {
					logger.error("action [" + action + "] request error: ", e);
					throw new ValueException("action [" + action + "] request error: " + e.getMessage());
				}	
			}
			
			try {
				response = action(deviceId, action, requestValue);
				if(!response.isSuccess())
					throw new StatusException(response.getError());
				else {
					if(actionType.getResponse() != null) {
						responseDataType = actionType.getResponseAttributeType().getDataType();
						return getResponse();
					}
					else
						return null;
				}
			} catch(Throwable e) {
				logger.error("action [" + action + "] response error: ", e);
				throw new ValueException("action [" + action + "] response error: " + e.getMessage());
			}
		}
		
		@SuppressWarnings("unchecked")
		protected T getResponse() {
			if(responseTypeClass != null)
				return  (T) responseDataType.getData(response.getValue(), responseTypeClass);
			else if(responseType != null)
				return  (T) responseDataType.getData(response.getValue(), responseType);
			else
				return null;
		}
		
		private Response action(String deviceId, String action, AttValueInfo value) {
			RequestAction request = new RequestAction(deviceId, action, value);
	        
	        connect(request);
			
	        try {
				return request.get(timeout, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new StatusException(e.getMessage());
			}
		}
	}
}
