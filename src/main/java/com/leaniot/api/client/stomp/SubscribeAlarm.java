package com.leaniot.api.client.stomp;

import java.lang.reflect.Type;

import org.springframework.messaging.simp.stomp.StompHeaders;

import com.leaniot.api.stomp.SubscribeHandler;
import com.leaniot.domain.Alarm;

public class SubscribeAlarm extends SubscribeHandler {
	public SubscribeAlarm(String deviceId, AlarmSubscriber subscriber) {
		super(deviceId, subscriber);
	}

	@Override
	public Type getPayloadType(StompHeaders headers) {
		return Alarm.class;
	}

	@Override
	public String getTopic() {
		// TODO Auto-generated method stub
		return "alarm";
	}
}