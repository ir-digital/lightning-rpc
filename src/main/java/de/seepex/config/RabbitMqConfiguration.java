package de.seepex.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.seepex.domain.RpcRabbitTransport;
import de.seepex.util.SeepexMessageConverter;
import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class RabbitMqConfiguration {

    @Value("${spx.com.rabbit.rpc.host}")
    private String host;

    @Value("${spx.com.rabbit.rpc.port}")
    private int port;

    @Value("${spx.com.rabbit.rpc.username}")
    private String username;

    @Value("${spx.com.rabbit.rpc.password}")
    private String password;

    @Value("${spx.com.rabbit.rpc.concurrency:10}")
    private String concurrency;

    @Lazy
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Bean
    public DirectExchange deviceExchange() {
        return new DirectExchange("rpc-direct");
    }

    @Bean
    public Queue replyQueue() {
        return new AnonymousQueue();
    }

    private static final Long TIMEOUT = 60000L;

    @Bean
    public AsyncRabbitTemplate template() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(replyQueue().getName());
        container.setConcurrency(concurrency);

        rabbitTemplate.setReplyAddress(replyQueue().getName());
        rabbitTemplate.setReplyTimeout(TIMEOUT); // large enough to load, prepare and transfer big files over amqp
        AsyncRabbitTemplate asyncRabbitTemplate = new AsyncRabbitTemplate(rabbitTemplate, container);
        asyncRabbitTemplate.setReceiveTimeout(TIMEOUT); // large enough to load, prepare and transfer big files over amqp
        return asyncRabbitTemplate;
    }

    @Bean
    public RpcRabbitTransport rpcRabbitTransport() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        AnonymousQueue replyQueue = new AnonymousQueue();
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
        rabbitAdmin.setAutoStartup(true);
        rabbitAdmin.declareQueue(replyQueue);
        rabbitAdmin.declareExchange(new DirectExchange("rpc-direct"));

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueueNames(replyQueue.getName());
        container.setConcurrency(concurrency);
        container.start();

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setReplyAddress(replyQueue.getName());
        rabbitTemplate.setReplyTimeout(TIMEOUT); // large enough to load, prepare and transfer big files over amqp
        rabbitTemplate.start();

        AsyncRabbitTemplate asyncRabbitTemplate = new AsyncRabbitTemplate(rabbitTemplate, container);
        asyncRabbitTemplate.setReceiveTimeout(TIMEOUT); // large enough to load, prepare and transfer big files over amqp
        asyncRabbitTemplate.start();

        RpcRabbitTransport rpcRabbitTransport = new RpcRabbitTransport();
        rpcRabbitTransport.setRabbitAdmin(rabbitAdmin);
        rpcRabbitTransport.setAsyncRabbitTemplate(asyncRabbitTemplate);
        rpcRabbitTransport.setRabbitTemplate(rabbitTemplate);

        return rpcRabbitTransport;
    }

    @Bean
    public MessageConverter messageConverter(final ObjectMapper objectMapper) {
        return new SeepexMessageConverter(objectMapper);
    }
}