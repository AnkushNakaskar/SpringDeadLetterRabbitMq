package vc.c4.deadletter.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

public class OutGoingQueueConsumer implements MessageListener{

	public void onMessage(Message arg0) {
		System.out.println(".............................In Out going consumer...........................");
		throw new RuntimeException();
	}

}
