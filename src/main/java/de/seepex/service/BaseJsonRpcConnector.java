package de.seepex.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import de.seepex.annotation.SpxService;
import de.seepex.annotation.SpxServiceCommunicationDoc;
import de.seepex.domain.*;
import de.seepex.util.InvokePropertiesExtractor;
import de.seepex.util.InvokeResultConstructor;
import de.seepex.util.RpcContext;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.AmqpReplyTimeoutException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageBuilderSupport;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class BaseJsonRpcConnector {

    private final InvokeResultConstructor invokeResultConstructor = new InvokeResultConstructor();
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Value("${service-communication.device-exchange.direct:rpc-direct}")
    private String rpcExchange;

    @Autowired
    private RpcRabbitTransport rpcRabbitTransport;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RpcTools rpcTools;

    @Autowired
    private RpcEventService rpcEventService;

    @Autowired
    private MeterRegistry meterRegistry;

    /**
     *  Execute RPC call synchronously.
     *
     * <pre>All available methods: <a href="/api/scs-doc"> test </a> <a href="/api/scs-doc"> prod </a></pre>

     *
     * @param methodName {@link SpxServiceCommunicationDoc#methodName()}
     * @param serviceId {@link SpxService#id()}
     * @param params params of the method. Needs to be named as params in method definition
     * @return
     */
    public <T> T rpc(String methodName, String serviceId, Param... params) {
        return rpc(methodName, serviceId, RpcContext.getApplicationHeaders(), params);
    }

    /**
     *
     * @param methodName
     * @param serviceId
     * @param applicationHeaders - additional headers that will be made avaliable on the receiving end
     * @param params
     * @param <T>
     * @return
     */
    public <T> T rpc(String methodName, String serviceId, Map<String, String> applicationHeaders, Param... params) {
        Map<String, Object> request = rpcTools.getCommand(methodName, serviceId, params);
        String routingKey = rpcTools.getRoutingKey(serviceId);

        String callerClassName = "unknown";
        for (StackTraceElement stackTraceElement : new Exception().getStackTrace()) {
            String callingClass = stackTraceElement.getClassName();
            if (!callingClass.contains("BaseJsonRpcConnector")) {
                callerClassName = callingClass;
                break;
            }
        }

        CacheResult<T> inCache = rpcTools.findInCache(methodName, serviceId, params);
        if (Boolean.TRUE.equals(inCache.isFound())) {
            return inCache.getResult();
        }

        return this.executeRpcCall(request, routingKey, callerClassName, applicationHeaders);
    }

    /**
     *  Execute RPC call asynchronously
     *
     * <pre>All available methods: <a href="/api/scs-doc"> test </a> <a href="/api/scs-doc"> prod </a></pre>
     * <pre>To get response use {@link CompletableFuture#get()} or {@link CompletableFuture#allOf(CompletableFuture[])} ()} to wait for more than one Future </pre>
     *
     * @param methodName {@link SpxServiceCommunicationDoc#methodName()}
     * @param serviceId {@link SpxService#id()}
     * @param params params of the method. Needs to be named as params in method definition
     * @return
     */
    public <T> CompletableFuture<T> rpcAsync(String methodName, String serviceId, Param... params) {
        Map<String, Object> request = rpcTools.getCommand(methodName, serviceId, params);
        String routingKey = rpcTools.getRoutingKey(serviceId);
        String callerClassName = new Exception().getStackTrace()[1].getClassName();

        // try to load from cache, otherwise we will make the rpc call
        CacheResult<T> inCache = rpcTools.findInCache(methodName, serviceId, params);

        if(Boolean.TRUE.equals(inCache.isFound())){
            return CompletableFuture.completedFuture(inCache.getResult());
        }
        return this.executeRpcCallAsync(request, routingKey, callerClassName);
    }

    private Message getRpcMessage(Map<String, Object> command, Map<String, String> applicationHeaders, String callerClassName) {
        try {
            MessageBuilderSupport<Message> messageBuilder = MessageBuilder
                    .withBody(objectMapper.writeValueAsBytes(command))
                    .setHeader(Headers.RPC_INVOKED_AT.name(), new Date().getTime())
                    .setHeader(Headers.USER_ID.name(), rpcTools.getLoggedInUserId())
                    .setHeader(Headers.CALLER_CLASS.name(), callerClassName)
                    .setHeader(Headers.CALLER_HOSTNAME.name(), rpcTools.getHostName())
                    .setHeader(Headers.CALLER_SERVICE.name(), rpcTools.getServiceName());

            if(applicationHeaders != null && applicationHeaders.containsKey("correlationId")) {
                messageBuilder.setCorrelationIdIfAbsent(applicationHeaders.get("correlationId"));
            } else {
                messageBuilder.setCorrelationIdIfAbsent(UUID.randomUUID().toString());
            }

            if(applicationHeaders != null && !applicationHeaders.isEmpty()) {
                messageBuilder.setHeader(Headers.APPLICATION_HEADERS.name(), new Gson().toJson(applicationHeaders));
            }

            return messageBuilder.build();
        } catch (Exception e) {
            LOG.error("Failed to construct message", e);
        }

        return null;
    }

    /**
     * Executes the RPC call
     *
     * @param command
     * @param routingKey
     * @param callerClassName
     * @param applicationHeaders - additional headers that will be made avaliable on the receiving end in thread local
     * @param <T>
     *
     * @return
     */
    private <T> T executeRpcCall(Map<String, Object> command, String routingKey, String callerClassName, Map<String, String> applicationHeaders) {
        String serviceId = (String) command.get("service_id");
        String methodName = (String) command.get("method");

        String correlationId = null;
        try {
            Message rpcMessage = getRpcMessage(command, applicationHeaders, callerClassName);
            if(rpcMessage == null) {
                return null;
            }
            correlationId = rpcMessage.getMessageProperties().getCorrelationId();

            long rpcOperationStartedAt = System.currentTimeMillis();

            AsyncRabbitTemplate.RabbitMessageFuture rabbitMessageFuture = rpcRabbitTransport.getAsyncRabbitTemplate().sendAndReceive(rpcExchange, routingKey, rpcMessage);
            Message message = rabbitMessageFuture.get();

            return this.handleResponse(message, rpcOperationStartedAt, serviceId, methodName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Thread interrupted. Failed to execute command " + serviceId + "." + methodName, e);
        } catch (Exception e) {
            this.handleException(e, correlationId, serviceId, methodName, command);
        }

        return null;
    }

    private void handleException(Throwable e, String correlationId, String serviceId, String methodName, Map<String, Object> command) {
        User user = rpcTools.getUser();
        if(user != null) {
            MDC.put("username", user.getUsername());
        }

        meterRegistry.counter("rpc.failed", "service", serviceId, "method", methodName).increment();
        RpcEvent rpcEvent = rpcEventService.getEvent(correlationId);

        LOG.error("Failed to execute command " + serviceId + "." + methodName + ". Call receive event: " + rpcEvent, e);
        if (e instanceof ExecutionException) {
            final ExecutionException ee = (ExecutionException) e;
            final Throwable cause = ee.getCause();
            if (cause instanceof AmqpReplyTimeoutException) {
                LOG.error("Can't connect to {} to execute {}.{} with command map {}. Call receive Event {}", serviceId, serviceId, methodName, command, rpcEvent);
            }
            LOG.error(cause.getMessage(), cause);
        }

        LOG.error(e.getMessage(), e);
        MDC.clear();
    }

    private <T> CompletableFuture<T> executeRpcCallAsync(Map<String, Object> command, String routingKey, String callerClassName) {
        String serviceId = (String) command.get("service_id");
        String methodName = (String) command.get("method");
        final String correlationId;

        try {
            Message rpcMessage = getRpcMessage(command, new HashMap<>(), callerClassName);
            if(rpcMessage == null) {
                return null;
            }
            correlationId = rpcMessage.getMessageProperties().getCorrelationId();

            long rpcOperationStartedAt = System.currentTimeMillis();
            AsyncRabbitTemplate.RabbitMessageFuture rabbitMessageFuture = rpcRabbitTransport.getAsyncRabbitTemplate().sendAndReceive(rpcExchange, routingKey, rpcMessage);

            return rabbitMessageFuture.completable()
                    .handle((future, exception) -> {
                        if (exception == null) {
                            return handleResponse(future, rpcOperationStartedAt, serviceId, methodName);
                        } else {
                            handleException(exception, correlationId, serviceId, methodName, command);
                            return null;
                        }
                    });
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return CompletableFuture.completedFuture(null);
    }

    private <T> T handleResponse(Message message, long rpcOperationStartedAt, String serviceId, String methodName) {
        try {

            // decompress
            String responsePayload = rpcTools.decompress(message);
            if(responsePayload == null) {
                return null;
            }

            String methodReturnType = (String) message.getMessageProperties().getHeaders().get(Headers.INVOKED_METHOD_RETURN_TYPE.name());
            List<String> typeHints = rpcTools.getTypeHints(message);

            rpcTools.trackStatistics(message, rpcOperationStartedAt, serviceId, methodName);

            if (StringUtils.isEmpty(responsePayload)) {
                return null;
            }

            if (StringUtils.isEmpty(methodReturnType)) {
                return (T) responsePayload;
            }

            final MetaReturn metaReturn = InvokePropertiesExtractor.getReturnClass(methodReturnType);
            final Class returnClass = metaReturn.getReturnedBaseClass();

            // that means the returned class could not be mapped to a known entity.
            if(returnClass == null) {
                return (T) rpcTools.getGenericResponse(responsePayload);
            }

            if (InvokePropertiesExtractor.isElement(returnClass)) {
                Object element = invokeResultConstructor.getElement(returnClass, responsePayload);
                return (T) element;
            }

            if (InvokePropertiesExtractor.isList(returnClass)) {
                String contentClass = InvokePropertiesExtractor.containerContent(methodReturnType);
                List<T> list = invokeResultConstructor.getList(contentClass, responsePayload, typeHints);
                return (T) list;
            }

            if (InvokePropertiesExtractor.isSet(returnClass)) {
                String contentClass = InvokePropertiesExtractor.containerContent(methodReturnType);
                Set<T> set = invokeResultConstructor.getSet(contentClass, responsePayload);
                return (T) set;
            }

            if (InvokePropertiesExtractor.isPage(returnClass)) {
                String contentClass = InvokePropertiesExtractor.containerContent(methodReturnType);
                Page<T> page = invokeResultConstructor.getPage(contentClass, responsePayload);
                return (T) page;
            }

            if (InvokePropertiesExtractor.isMap(returnClass)) {
                Map map = invokeResultConstructor.getMap(metaReturn, responsePayload, true);
                return (T) map;
            }

            if (InvokePropertiesExtractor.isVoid(returnClass)) {
                return null;
            }


        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }

        return null;
    }
}
