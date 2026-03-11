package com.sigpqr.auth.config;

import com.sigpqr.common.constants.RabbitMQConstants;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration for the auth service.
 *
 * <p>Declares the {@code auth.events} topic exchange used to publish
 * authentication-related events (e.g. password reset requests).
 * Consumer queues and bindings are declared by the notification-service.</p>
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Topic exchange for auth domain events.
     *
     * @return a durable {@link TopicExchange} named {@code auth.events}
     */
    @Bean
    public TopicExchange authEventsExchange() {
        return new TopicExchange(RabbitMQConstants.AUTH_EVENTS_EXCHANGE);
    }

    /**
     * Jackson-based message converter for serializing/deserializing
     * RabbitMQ message payloads as JSON.
     *
     * @return a {@link Jackson2JsonMessageConverter} instance
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
