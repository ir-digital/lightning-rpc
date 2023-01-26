package de.seepex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.seepex.domain.*;
import de.seepex.util.*;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ConditionalOnProperty(name = "provide-rpc", havingValue = "true")
public class JsonRpcService extends BasicJsonRpcProvider {

    private final RpcRabbitTransport rpcRabbitTransport;

    @Value("${gzip-payload:false}")
    private Boolean gzipPayload;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final AnnotationExtractor annotationExtractor;
    private final RpcTools rpcTools;
    private final RpcEventService rpcEventService;

    // effectively final tricks for executor Lambda down below
    private AbstractMessageListenerContainer container;

    private Gson gson;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    public JsonRpcService(ApplicationContext applicationContext, RpcRabbitTransport rpcRabbitTransport,
                          AnnotationExtractor annotationExtractor, RpcTools rpcTools, RpcEventService rpcEventService) {
        super(applicationContext);
        this.rpcRabbitTransport = rpcRabbitTransport;
        this.annotationExtractor = annotationExtractor;
        this.rpcTools = rpcTools;
        this.rpcEventService = rpcEventService;

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapterFactory(PersistentCollectionCheckingTypeAdapter.FACTORY);
        gson = gsonBuilder.create();
    }

    @PostConstruct
    public void initialize() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(new MapSerializer());
        objectMapper.registerModule(module);

        String serviceName = AnnotationExtractor.getServiceName(applicationContext);
        String queueName = AnnotationExtractor.getQueueName(serviceName);
        String routingKey = AnnotationExtractor.getRoutingKey(serviceName);

        // default rpc queue
        attachToQueue(queueName, routingKey, false);

        // exclusive access rpc queue
        if(annotationExtractor.hasExclusiveServices()) {
            attachToQueue(queueName + "_exclusive", routingKey + "_exclusive", true);
        }
    }

    private void attachToQueue(String queueName, String routingKey, boolean autodelete) {
        QueueBuilder queueBuilder = QueueBuilder.durable(queueName).withArgument("x-message-ttl", 10000);
        if(autodelete) {
            queueBuilder.autoDelete();
        }

        Queue queue = queueBuilder.build();

        //ConnectionFactory connectionFactory = applicationContext.getBean(ConnectionFactory.class);
        RabbitAdmin rabbitAdmin = rpcRabbitTransport.getRabbitAdmin();

        container =  getMessageListenerContainer(routingKey, queue, rabbitAdmin);

        executor.execute(() -> {
            try {
                while(true && container != null) {
                    try {
                        Thread.sleep(5000L);
                    } catch (final InterruptedException e) {
                        logger.debug("Thread sleep interrupted...");
                    }
                    if(!container.isActive()) {
                        logger.warn("Restart of MessageListener required, triggering it now.");
                        container = getMessageListenerContainer(routingKey, queue, rabbitAdmin);
                    }
                }
            } catch (final Throwable e) {
                logger.error("ContainerListener protection failed ...", e);
            }
        });
    }

    /**
     * This supports old style requests, where params were submitted as a hashmap
     * instead of using param objects. SCS does not use this anymore, but other projects (NestJS, .net) still do
     *
     * @param request
     * @param messageJson
     */
    private void updateParamsFromLegacyRequest(RpcRequest request, String messageJson) {
        List<Param> params = new ArrayList<>();

        if(!request.getParams().isEmpty() && request.getParams().get(0).getName() == null) {

            logger.info("Mapping legacy request. RpcRequest {}, jsonPayload {}, callerClass {} ", request, messageJson, RpcContext.getCallerClass());

            HashMap legacyRequest;
            try {
                legacyRequest = objectMapper.readValue(messageJson, HashMap.class);
            } catch (JsonProcessingException e) {
                logger.error("Mapping failed", e);
                return;
            }
            List<Map> legacyParams = (List<Map>) legacyRequest.get("params");

            for (Map legacyParam : legacyParams) {
                Set<Map.Entry<String, Object>> set = legacyParam.entrySet();
                Optional<Map.Entry<String, Object>> first = set.stream().findFirst();
                Param param = new Param(first.get().getKey(), first.get().getValue());
                params.add(param);
            }

            request.setParams(params);
        }
    }

    private AbstractMessageListenerContainer getMessageListenerContainer(String routingKey, Queue queue, RabbitAdmin rabbitAdmin) {
        return startListening(rabbitAdmin, queue,
                new DirectExchange("rpc-direct"), routingKey, message -> {
                    String messageJson = (new String(message.getBody()));
                    RpcRequest request = null;
                    try {
                        request = objectMapper.readValue(messageJson, RpcRequest.class);

                        MessageProperties messageProperties = message.getMessageProperties();

                        MessageProperties responseProperties = new MessageProperties();
                        responseProperties.setContentType(MediaType.TEXT_PLAIN.toString());
                        responseProperties.setContentEncoding(StandardCharsets.UTF_8.name());

                        if (messageProperties.getCorrelationId() != null) {
                            responseProperties.setCorrelationId(messageProperties.getCorrelationId());
                        }

                        // rabbit RTT stats
                        if(messageProperties.getHeaders().containsKey(Headers.RPC_INVOKED_AT.name())) {
                            long invokedAt = (Long) messageProperties.getHeaders().get(Headers.RPC_INVOKED_AT.name());
                            long duration = System.currentTimeMillis() - invokedAt;
                            responseProperties.setHeader(Headers.RPC_RTT.name(), duration);
                        }

                        // if userId was passed in the header, we make it available to the app
                        if(messageProperties.getHeaders().containsKey(Headers.USER_ID.name())) {
                            String userId = (String) messageProperties.getHeaders().get(Headers.USER_ID.name());
                            RpcContext.setUserId(userId);
                        }

                        // hostname
                        if(messageProperties.getHeaders().containsKey(Headers.CALLER_HOSTNAME.name())) {
                            String hostname = (String) messageProperties.getHeaders().get(Headers.CALLER_HOSTNAME.name());
                            RpcContext.setHostname(hostname);
                        }

                        // additional application headers
                        HashMap<String, String> applicationHeaders = null;
                        if(messageProperties.getHeaders().containsKey(Headers.APPLICATION_HEADERS.name())) {
                            String applicationHeadersPayload = (String)messageProperties.getHeaders().get(Headers.APPLICATION_HEADERS.name());
                            applicationHeaders = new Gson().fromJson(applicationHeadersPayload, HashMap.class);
                            RpcContext.setApplicationHeaders(applicationHeaders);
                        }

                        // track statistics
                        String callerClass = "unknown";
                        if(messageProperties.getHeaders().containsKey(Headers.CALLER_CLASS.name())) {
                            callerClass = (String)messageProperties.getHeaders().get(Headers.CALLER_CLASS.name());
                            RpcContext.setCallerClass(callerClass);
                        }

                        // legacy call
                        updateParamsFromLegacyRequest(request, messageJson);

                        this.meterRegistry.counter("rpc.rabbit.caller", "service", request.getServiceId(), "method", request.getMethod(), "source", callerClass).increment();

                        // report received call
                        if(messageProperties.getCorrelationId() != null &&
                                messageProperties.getHeaders().containsKey(Headers.CALLER_SERVICE.name())) {
                            final String correlationId = messageProperties.getCorrelationId();
                            final String callerService = (String)messageProperties.getHeaders().get(Headers.CALLER_SERVICE.name());
                            rpcEventService.rpcCallReceived(correlationId, callerService);
                        }

                        // call method
                        InvokeResult invokeResult = serve(request, applicationHeaders);

                        // something went completely wrong....
                        if(invokeResult == null) {
                            logger.error("Call {}.{} failed. CallerClass {}", request.getServiceId(), request.getMethod(), callerClass);
                            return;
                        }

                        // processing instance data
                        responseProperties.setHeader(Headers.REPLIED_BY_HOSTNAME.name(), rpcTools.getHostName());

                        // target method threw an exception
                        if(invokeResult.isFailed()) {
                            responseProperties.setHeader(Headers.EXCEPTION_TEXT.name(), invokeResult.getExceptionText());
                            responseProperties.setHeader(Headers.EXCEPTION_CLASS_NAME.name(), invokeResult.getExceptionClassName());

                            Message responseMessage = new Message(null, responseProperties);
                            rpcRabbitTransport.getRabbitTemplate().send(messageProperties.getReplyTo(), responseMessage);

                            logger.error("Call {}.{} failed. CallerClass {}", request.getServiceId(), request.getMethod(), callerClass);

                            return;
                        }

                        String resultAsJson = "";
                        final Object response = invokeResult.getResponse();

                        Class returnClass = InvokePropertiesExtractor.getReturnClass(invokeResult.getMethod());

                        // we want our custom serializer module only for hashmaps
                        if(response != null) {
                            if(InvokePropertiesExtractor.isMap(returnClass) || InvokePropertiesExtractor.isPage(returnClass)) {
                                resultAsJson = objectMapper.writeValueAsString(response);
                            } else {
                                resultAsJson = gson.toJson(response);
                            }
                        }

                        // set hints if any provided
                        // be aware that it seems, that there is a max header size of 128kB for rabbit per default, so we might run into issues with large lists
                        // 128kB == 128.000 characters, so actually a plenty of space...
                        if(!invokeResult.getResponseTypeHints().isEmpty()) {
                            responseProperties.setHeader(Headers.TYPE_HINTS.name(), StringUtils.join(invokeResult.getResponseTypeHints(), "|"));
                        }

                        // pass called method return type back to caller
                        responseProperties.setHeader(Headers.INVOKED_METHOD_RETURN_TYPE.name(), InvokePropertiesExtractor.getReturnType(invokeResult.getMethod()));
                        responseProperties.setHeader(Headers.RPC_INVOKED_AT.name(), System.currentTimeMillis());
                        if(resultAsJson != null) {
                            responseProperties.setHeader(Headers.PAYLOAD_SIZE.name(), resultAsJson.getBytes(StandardCharsets.UTF_8).length);
                        }

                        // when content gzip is enabled
                        byte[] compress = resultAsJson.getBytes(StandardCharsets.UTF_8);
                        if(gzipPayload) {
                            responseProperties.setHeader(Headers.IS_COMPRESSED.name(), true);
                            compress = Base64GZipCompression.compress(resultAsJson);
                        }

                        Message responseMessage = new Message(compress, responseProperties);
                        rpcRabbitTransport.getRabbitTemplate().send(messageProperties.getReplyTo(), responseMessage);
                    } catch (Throwable e) {
                        logger.error("Failed to serve request. Request: " + request, e);
                    } finally {
                        RpcContext.clear();
                        ContextAwareUtil.clear();
                    }
                });
    }

    private AbstractMessageListenerContainer startListening(RabbitAdmin rpcRabbitAdmin, Queue queue, Exchange exchange, String key, MessageListener messageListener) {
        rpcRabbitAdmin.declareQueue(queue);
        rpcRabbitAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(key).noargs());
        
        SimpleMessageListenerContainer listener = new SimpleMessageListenerContainer(rpcRabbitAdmin.getRabbitTemplate().getConnectionFactory());
        listener.addQueues(queue);
        listener.setMessageListener(messageListener);
        listener.setConcurrency("10");
        listener.start();

        return listener;
    }

}

