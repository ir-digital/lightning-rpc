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
import java.util.concurrent.ExecutionException;

@Component
public class BaseJsonRpcConnectorV2<T> {

    private final InvokeResultConstructor invokeResultConstructor = new InvokeResultConstructor();

    private static final Logger LOG = LoggerFactory.getLogger(BaseJsonRpcConnectorV2.class);

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
    public RpcResponse<T> rpc(String methodName, String serviceId, Param... params) {
        return rpc(methodName, serviceId, RpcContext.getApplicationHeaders(), null, params);
    }

    public RpcResponse<T> rpc(String methodName, String serviceId, Map<String, String> applicationHeaders, Param... params) {
        return rpc(methodName, serviceId, applicationHeaders, null, params);
    }

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
    public RpcResponse<T> rpc(String methodName, String serviceId, Integer responseTimeoutMilliseconds, Param... params) {
        return rpc(methodName, serviceId, RpcContext.getApplicationHeaders(), responseTimeoutMilliseconds, params);
    }

    /**
     *
     * @param methodName
     * @param serviceId
     * @param applicationHeaders - additional headers that will be made avaliable on the receiving end
     * @param params
     * @param
     * @return
     */
    public RpcResponse<T> rpc(String methodName, String serviceId, Map<String, String> applicationHeaders, Integer responseTimeoutMilliseconds, Param... params) {
        RpcRequest request = rpcTools.getRequest(methodName, serviceId, params);
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
            new RpcResponse<>(inCache.getResult());
        }

        return this.executeRpcCall(request, routingKey, callerClassName, applicationHeaders, responseTimeoutMilliseconds);
    }

    private Message getRpcMessage(RpcRequest request, Map<String, String> applicationHeaders, String callerClassName) {
        try {
            MessageBuilderSupport<Message> messageBuilder = MessageBuilder
                    .withBody(objectMapper.writeValueAsBytes(request))
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
     * @param request
     * @param routingKey
     * @param callerClassName*
     * @param applicationHeaders - additional headers that will be made avaliable on the receiving end in thread local
     * @param responseTimeoutMilliseconds - allows setting a custom response timeout. This can be useful if we can omit a RPC
     *                                    response. For example when working with UI and we can live with parts not being there 
     *
     * @return
     */
    private RpcResponse<T> executeRpcCall(RpcRequest request, String routingKey, String callerClassName,
                                          Map<String, String> applicationHeaders, Integer responseTimeoutMilliseconds) {
        String correlationId = null;
        try {
            Message rpcMessage = getRpcMessage(request, applicationHeaders, callerClassName);
            if(rpcMessage == null) {
                return null;
            }
            correlationId = rpcMessage.getMessageProperties().getCorrelationId();

            long rpcOperationStartedAt = System.currentTimeMillis();

            AsyncRabbitTemplate asyncRabbitTemplate = rpcRabbitTransport.getAsyncRabbitTemplate();
            if(responseTimeoutMilliseconds != null) {
                asyncRabbitTemplate.setReceiveTimeout(responseTimeoutMilliseconds);
            }

            AsyncRabbitTemplate.RabbitMessageFuture rabbitMessageFuture = asyncRabbitTemplate.sendAndReceive(rpcExchange, routingKey, rpcMessage);
            Message message = rabbitMessageFuture.get();

            return this.handleResponse(message, rpcOperationStartedAt, request.getServiceId(), request.getMethod());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Thread interrupted. Failed to execute command " + request.getServiceId() + "." + request.getMethod(), e);
        } catch (Exception e) {
            // something failed inside of the execution method
            return this.handleException(e, correlationId, request);
        }

        // this could be a timeout, wrong configuration or other causes, where we received no response at all
        RpcResponse<T> rpcResponse = new RpcResponse<>();
        rpcResponse.setFailed(true);

        this.meterRegistry.counter("rpc.failed", "service", request.getServiceId(), "method", request.getMethod()).increment();

        return rpcResponse;
    }

    private RpcResponse<T> handleException(Throwable e, String correlationId, RpcRequest request) {
        RpcResponse<T> rpcResponse = new RpcResponse<>();
        rpcResponse.setFailed(true);

        User user = rpcTools.getUser();
        if(user != null) {
            MDC.put("username", user.getUsername());
        }

        this.meterRegistry.counter("rpc.failed", "service", request.getServiceId(), "method", request.getMethod()).increment();
        RpcEvent rpcEvent = rpcEventService.getEvent(correlationId);

        LOG.error("Failed to execute request " + request.getServiceId() + "." + request.getMethod() + ". Call receive event: " + rpcEvent, e);
        if (e instanceof ExecutionException) {
            final ExecutionException ee = (ExecutionException) e;
            final Throwable cause = ee.getCause();

            rpcResponse.setExceptionText(e.getMessage());
            rpcResponse.setExceptionClassName(e.getClass().getName());

            if (cause instanceof AmqpReplyTimeoutException) {
                LOG.error("Can't connect to {} to execute {}.{} with request map {}. Receive event {}", request.getServiceId(),
                        request.getServiceId(), request.getMethod(), request, rpcEvent);
                rpcResponse.setExceptionText(cause.getMessage());
                rpcResponse.setExceptionClassName(cause.getClass().getName());
            }
            LOG.error(cause.getMessage(), cause);
        }

        LOG.error(e.getMessage(), e);
        MDC.clear();

        return rpcResponse;
    }

    private RpcResponse<T> handleResponse(Message message, long rpcOperationStartedAt, String serviceId, String methodName) {
        String failureText = StringUtils.EMPTY;
        
        try {
            // execution failed with an exception on the remote side
            if (message.getMessageProperties().getHeaders().containsKey(Headers.EXCEPTION_TEXT.name())) {
                String exceptionText = (String) message.getMessageProperties().getHeaders().get(Headers.EXCEPTION_TEXT.name());
                String exceptionClassName = (String) message.getMessageProperties().getHeaders().get(Headers.EXCEPTION_CLASS_NAME.name());

                RpcResponse<T> rpcResponse = new RpcResponse<>();
                rpcResponse.setFailed(true);
                rpcResponse.setExceptionText(exceptionText);
                rpcResponse.setExceptionClassName(exceptionClassName);

                return rpcResponse;
            }

            // decompress
            String responsePayload = rpcTools.decompress(message);
            if(responsePayload == null) {
                RpcResponse<T> rpcResponse = new RpcResponse<>();
                rpcResponse.setFailed(true);
                return rpcResponse;
            }

            String methodReturnType = (String) message.getMessageProperties().getHeaders().get(Headers.INVOKED_METHOD_RETURN_TYPE.name());
            List<String> typeHints = rpcTools.getTypeHints(message);

            rpcTools.trackStatistics(message, rpcOperationStartedAt, serviceId, methodName);

            if (StringUtils.isEmpty(responsePayload)) {
                return new RpcResponse<>();
            }

            if (StringUtils.isEmpty(methodReturnType)) {
                return new RpcResponse<>((T)responsePayload);
            }

            final MetaReturn metaReturn = InvokePropertiesExtractor.getReturnClass(methodReturnType);
            final Class returnClass = metaReturn.getReturnedBaseClass();

            // that means the returned class could not be mapped to a known entity.
            if(returnClass == null) {
                GenericResponse genericResponse = rpcTools.getGenericResponse(responsePayload);
                return new RpcResponse(genericResponse);
            }

            if (InvokePropertiesExtractor.isElement(returnClass)) {
                Object element = invokeResultConstructor.getElement(returnClass, responsePayload);
                return new RpcResponse<>((T)element);
            }

            if (InvokePropertiesExtractor.isList(returnClass)) {
                final Class containerClass = metaReturn.getGenerics().get(0);
                List<T> list = invokeResultConstructor.getList(containerClass.getName(), responsePayload, typeHints);
                return new RpcResponse<>((T) list);
            }

            if (InvokePropertiesExtractor.isSet(returnClass)) {
                final Class containerClass = metaReturn.getGenerics().get(0);
                Set<T> set = invokeResultConstructor.getSet(containerClass.getName(), responsePayload);
                return new RpcResponse<>((T) set);
            }

            if (InvokePropertiesExtractor.isPage(returnClass)) {
                final Class containerClass = metaReturn.getGenerics().get(0);
                Page<T> page = invokeResultConstructor.getPage(containerClass.getName(), responsePayload);
                return new RpcResponse<>((T) page);
            }

            if (InvokePropertiesExtractor.isMap(returnClass)) {
                Map map = invokeResultConstructor.getMap(metaReturn, responsePayload, true);
                return new RpcResponse<>((T) map);
            }

            if (InvokePropertiesExtractor.isVoid(returnClass)) {
                return null;
            }
            
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            failureText = e.getMessage();
        }

        RpcResponse<T> failedResponse = new RpcResponse<>();
        failedResponse.setFailed(true);
        failedResponse.setExceptionText(failureText);
        return failedResponse;
    }
}
