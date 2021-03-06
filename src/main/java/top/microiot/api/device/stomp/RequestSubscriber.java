package top.microiot.api.device.stomp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import top.microiot.api.device.WebsocketDeviceSession;
import top.microiot.api.stomp.AbstractEventSubscriber;
import top.microiot.api.stomp.SessionManager;
import top.microiot.domain.Device;
import top.microiot.domain.Request;
import top.microiot.domain.Response;

public abstract class RequestSubscriber extends AbstractEventSubscriber {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	protected Request request;
	private WebsocketDeviceSession websocketDeviceSession;
	private Device device;
	
	public RequestSubscriber() {
		super();
	}
	public WebsocketDeviceSession getWebsocketDeviceSession() {
		return websocketDeviceSession;
	}
	public void setWebsocketDeviceSession(WebsocketDeviceSession websocketDeviceSession) {
		this.websocketDeviceSession = websocketDeviceSession;
	}
	public Device getDevice() {
		return device;
	}
	public void setDevice(Device device) {
		this.device = device;
	}
	public abstract Response getResponse();
	@Override
	public void onEvent(Object event) {
		request = (Request)event;
		logger.debug("request: " + request);
	}
	@Override
	public SessionManager getSessionManager() {
		return websocketDeviceSession;
	}

}
