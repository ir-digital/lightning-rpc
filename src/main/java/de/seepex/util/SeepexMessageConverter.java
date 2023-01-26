package de.seepex.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;

import java.util.stream.Collectors;

/**
 * Developer      : Tom Kornelson
 * Date           : 22.10.20
 * <p>
 * Project        : connected-services
 * Small Describe :
 **/
public class SeepexMessageConverter extends Jackson2JsonMessageConverter {

    private final ObjectMapper mapper;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SeepexMessageConverter(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        final MessageProperties messageProperties = message.getMessageProperties();
        if (messageProperties != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("******************************************");
                logger.debug("Receive Message :");
                final String correlationId = messageProperties.getCorrelationId();
                logger.debug("\t[{}]< ContentEncoding {}", correlationId, messageProperties.getContentEncoding());
                logger.debug("\t[{}]< ContentType {}", correlationId, messageProperties.getContentType());
                logger.debug("\t[{}]< ContentLength {}|{}", correlationId, messageProperties.getContentLength(), message.getBody().length);
                logger.debug("\t[{}]< ConsumerQueue {}", correlationId, messageProperties.getConsumerQueue());
                logger.debug("\t[{}]< ReceivedRoutingKey {}", correlationId, messageProperties.getReceivedRoutingKey());
                logger.debug("\t[{}]< ReceivedExchange {}", correlationId, messageProperties.getReceivedExchange());
                logger.debug("\t[{}]< Headers {}", correlationId, messageProperties.getHeaders().entrySet().stream()
                        .map(entry -> entry.getKey() + " - " + entry.getValue())
                        .collect(Collectors.joining(", ")));
                logger.debug("\t[{}]< Body {}", correlationId, new String(message.getBody()));
                logger.debug("******************************************");
            }

            String contentType = messageProperties.getContentType();
            if (contentType != null && contentType.contains("application/octet")) {
                return message.getBody();
            }

            if (contentType != null && contentType.contains("text/plain")) {
                return new String(message.getBody());
            }

            // Dirty Diana Fix, seems that we get geo data from "geolocations-to-deviceservice" without header
            if (contentType == null) {
                return message.getBody();
            }
        }
        return super.fromMessage(message);
    }

    @Override
    protected Message createMessage(Object objectToConvert, MessageProperties messageProperties) throws MessageConversionException {

        /**
         * Enabling this code will put the plain byte[] to the message. But we can not guarantee that some
         * backends on our side understand this directly.
         * Example : message_consumer.py will performe a Base64Decode on incoming messages to have the plain data.
         *      With this code enabled, the data is plain as bytes and not further encoded.
         */
        final boolean isArray = objectToConvert.getClass().isArray();
        final boolean isByteArray = isArray && objectToConvert.getClass().getComponentType().isAssignableFrom(byte.class);
        if (isArray && isByteArray) {
            logger.debug("Seems this is a protobuf message");
//            logger.debug("This is a byte array. Handling special.");
//            byte[] bytes = (byte[]) objectToConvert;
//            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_BYTES);
//            messageProperties.setContentLength(bytes.length);
//
//            if (super.getClassMapper() == null) {
//                getJavaTypeMapper().fromJavaType(mapper.constructType(objectToConvert.getClass()),
//                        messageProperties);
//
//            } else {
//                super.getClassMapper().fromClass(objectToConvert.getClass(),
//                        messageProperties);
//            }
//
//            return new Message(bytes, messageProperties);
        }
        logger.debug("creating rabbit message with default behavior.");

        if (logger.isDebugEnabled() && messageProperties != null) {
            logger.debug("******************************************");
            logger.debug("Prepare send Message :");
            final long currentTimeMillis = System.currentTimeMillis(); // Some Uniqueness to find grouped logs
            logger.debug("\t[{}]> ContentEncoding {}", currentTimeMillis, messageProperties.getContentEncoding());
            logger.debug("\t[{}]> ContentType {}", currentTimeMillis, messageProperties.getContentType());
            logger.debug("\t[{}]> ContentLength {}", currentTimeMillis, messageProperties.getContentLength());
            logger.debug("\t[{}]> ConsumerQueue {}", currentTimeMillis, messageProperties.getConsumerQueue());
            logger.debug("\t[{}]> ReceivedRoutingKey {}", currentTimeMillis, messageProperties.getReceivedRoutingKey());
            logger.debug("\t[{}]> ReceivedExchange {}", currentTimeMillis, messageProperties.getReceivedExchange());
            logger.debug("\t[{}]> Headers {}", currentTimeMillis, messageProperties.getHeaders().entrySet().stream()
                    .map(entry -> entry.getKey() + " - " + entry.getValue())
                    .collect(Collectors.joining(", ")));
            logger.debug("\t[{}]> Body {}", currentTimeMillis, objectToConvert);
            logger.debug("******************************************");
        }

        return super.createMessage(objectToConvert, messageProperties);
    }

}
