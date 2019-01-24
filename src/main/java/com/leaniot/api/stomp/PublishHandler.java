package com.leaniot.api.stomp;

import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import com.leaniot.api.dto.Request;
import com.leaniot.api.dto.Response;

public abstract class PublishHandler extends StompSessionHandlerAdapter implements Future<Response> {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private String deviceId;
	protected String topic;
	
	private volatile Response result = null;
    private volatile boolean cancelled = false;
    private final CountDownLatch countDownLatch;
    
	public PublishHandler(String deviceId) {
		super();
		this.deviceId = deviceId;
		this.countDownLatch = new CountDownLatch(1);
	}

	@Override
	public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
		synchronized(session) {
			String requestId = UUID.randomUUID().toString();
			String opTopic = "/topic/operation."+ topic + "."  + deviceId;
			String resultTopic = "/topic/result." + topic + "." + deviceId + "." + requestId;
			session.subscribe(resultTopic, this);
			Request request = getRequest(requestId);
			session.send(opTopic, request);
		}
	}
	
	protected abstract Request getRequest(String sessionId);
	
	@Override
	public Type getPayloadType(StompHeaders headers) {
		return Response.class;
	}

	@Override
	public void handleFrame(StompHeaders headers, Object payload) {
		synchronized(payload) {
			Response response = (Response)payload;
			
			this.result = response;
	        countDownLatch.countDown();
		}
	}
	
	@Override
	public boolean cancel(boolean arg0) {
		if (isDone()) {
            return false;
        } else {
            countDownLatch.countDown();
            cancelled = true;
            return !isDone();
        }
	}

	@Override
	public Response get() throws InterruptedException, ExecutionException {
		countDownLatch.await();
        return result;
	}

	@Override
	public Response get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		boolean finished = countDownLatch.await(timeout, unit);
		if(finished)
			return result;
		else
			throw new TimeoutException("timeout");
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isDone() {
		return countDownLatch.getCount() == 0;
	}
	
	@Override
	public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
			Throwable exception) {
		logger.error(exception.getMessage());
	}

}
