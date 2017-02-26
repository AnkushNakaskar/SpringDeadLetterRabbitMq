package vc.c4.deadletter.config;

import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.retry.RepublishMessageRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.interceptor.MethodInvocationRecoverer;
import org.springframework.retry.interceptor.RetryInterceptorBuilder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Configuration
public class MQConfig {

	public static final String OUTGOING_QUEUE = "outgoing.example";

	public static final String INCOMING_QUEUE = "incoming.example";

	@Autowired
	private ConnectionFactory cachingConnectionFactory;

	// Setting the annotation listeners to use the jackson2JsonMessageConverter

	@Bean
	public Advice[] advices() {
		ExponentialBackOffPolicy backoffPolicy = new ExponentialBackOffPolicy();
		backoffPolicy.setInitialInterval(10);
		backoffPolicy.setMaxInterval(1000);
		backoffPolicy.setMultiplier(2);
		Advice[] adviceChain = new Advice[1];
		List<Advice> listOfAdvices = new LinkedList<Advice>();
		Advice advice = RetryInterceptorBuilder.stateless().backOffPolicy(backoffPolicy).maxAttempts(3).build();
		listOfAdvices.add(advice);
		return listOfAdvices.toArray(adviceChain);
	}

	@Bean
	public SimpleMessageListenerContainer emailQueueListener() {
		SimpleMessageListenerContainer emailListener = new SimpleMessageListenerContainer();
		emailListener.setConnectionFactory(cachingConnectionFactory);
		emailListener.addQueueNames(new String[] { outgoingQueue().getName() });
		emailListener.setConcurrentConsumers(2);
		emailListener.setMessageListener(new OutGoingQueueConsumer());
		emailListener.setDefaultRequeueRejected(false);
		emailListener.setAdviceChain(advices());
		return emailListener;
	}

	// Standardize on a single objectMapper for all message queue items
	@Bean
	public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public Queue outgoingQueue() {
		Map<String, Object> args = new HashMap<String, Object>();
		// The default exchange
		args.put("x-dead-letter-exchange", "");
		// Route to the incoming queue when the TTL occurs
		args.put("x-dead-letter-routing-key", INCOMING_QUEUE);
		// TTL 5 seconds
		args.put("x-message-ttl", 5000);
		return new Queue(OUTGOING_QUEUE, false, false, false, args);
	}

	@Bean(name="outgoingSender")
	public RabbitTemplate outgoingSender() {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
		rabbitTemplate.setQueue(outgoingQueue().getName());
		rabbitTemplate.setRoutingKey(outgoingQueue().getName());
		rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
		return rabbitTemplate;
	}

	public RabbitTemplate incomingSender() {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
		rabbitTemplate.setQueue(incomingQueue().getName());
		rabbitTemplate.setRoutingKey(incomingQueue().getName());
		rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
		return rabbitTemplate;
	}

	@Bean
	public Queue incomingQueue() {
		return new Queue(INCOMING_QUEUE);
	}

	@Bean
	public SimpleMessageListenerContainer deadQueueListener() {
		SimpleMessageListenerContainer emailListener = new SimpleMessageListenerContainer();
		emailListener.setConnectionFactory(cachingConnectionFactory);
		emailListener.setAcknowledgeMode(AcknowledgeMode.AUTO);
		emailListener.addQueueNames(new String[] { incomingQueue().getName() });
		emailListener.setConcurrentConsumers(2);
		emailListener.setMessageListener(new IncomingQueueConsumer());
		emailListener.setAdviceChain(advices());
		return emailListener;
	}

}
