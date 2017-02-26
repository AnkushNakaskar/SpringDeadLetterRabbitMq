package vc.c4.deadletter.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

@Component
public class IncomingQueueConsumer implements MessageListener {

	public void onMessage(Message arg0) {
		System.out.println("........................................In incoming queue.......................................................................................................");
		
	}

}
