package de.seepex.domain;

import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RpcRabbitTransport {

    private RabbitAdmin rabbitAdmin;
    private AsyncRabbitTemplate asyncRabbitTemplate;
    private RabbitTemplate rabbitTemplate;

    public RabbitTemplate getRabbitTemplate() {
        return rabbitTemplate;
    }

    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public AsyncRabbitTemplate getAsyncRabbitTemplate() {
        return asyncRabbitTemplate;
    }

    public void setAsyncRabbitTemplate(AsyncRabbitTemplate asyncRabbitTemplate) {
        this.asyncRabbitTemplate = asyncRabbitTemplate;
    }

    public RabbitAdmin getRabbitAdmin() {
        return rabbitAdmin;
    }

    public void setRabbitAdmin(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }
}
