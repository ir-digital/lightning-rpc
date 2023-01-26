package de.seepex.service;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.seepex.domain.*;
import de.seepex.util.Base64GZipCompression;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.util.Pool;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RpcTools {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private RoutingCacheService routingCacheService;

    @Autowired
    @Qualifier(value = "serviceDocReadPool")
    private Pool<Jedis> jedisReadPool;

    /**
     * This carries the microservice name that is using the rpc framework
     * extracted from the EnableSpxRpc annotation
     */
    private final String serviceName;

    private static final Logger LOG = LoggerFactory.getLogger(RpcTools.class);

    public RpcTools(ApplicationContext applicationContext) {
        this.serviceName = AnnotationExtractor.getServiceName(applicationContext);
    }

    /**
     * Used by the old V1 rpc connector
     *
     */
    public Map<String, Object> getCommand(String methodName, String serviceId, Param... params) {
        // main request
        HashMap<String, Object> request = new HashMap<>();
        request.put("service_id", serviceId);
        request.put("method", methodName);

        // request params (method signature variables)
        List<HashMap> parameterList = new ArrayList<>();
        for (Param param : params) {

            HashMap parameter = new HashMap();

            // PageRequest requires custom handling
            if (param.getValue() instanceof PageRequest) {
                PageRequest pageRequest = (PageRequest) param.getValue();
                CustomPageable customPageable = CustomPageable.of(pageRequest);

                parameter.put(param.getName(), customPageable);
            } else {
                parameter.put(param.getName(), param.getValue());
            }

            parameterList.add(parameter);
        }
        request.put("params", parameterList);

        return request;
    }

    public RpcRequest getRequest(String methodName, String serviceId, Param... params) {
        // main request
        RpcRequest request = new RpcRequest();
        request.setServiceId(serviceId);
        request.setMethod(methodName);

        // request params (method signature variables)
        for (Param param : params) {
            // PageRequest requires custom handling
            if (param.getValue() instanceof PageRequest) {
                PageRequest pageRequest = (PageRequest) param.getValue();
                CustomPageable customPageable = CustomPageable.of(pageRequest);

                request.addParam(new Param(param.getName(), customPageable));
            } else {
                request.addParam(param);
            }
        }

        return request;
    }

    /**
     * Will resolve the correct routing key for the given serviceId
     * Routing information is shared among all services via redis pub/sub each 2 minutes.
     */
    public String getRoutingKey(String serviceId) {
        if (routingCacheService.getExclusiveRoutingCache().containsKey(serviceId)) {
            String routingKey = routingCacheService.getExclusiveRoutingCache().get(serviceId);
            if(routingKey != null) {
                return routingKey;
            }
        }

        // no hits for exclusive routing, so we check the default one
        if (routingCacheService.getRoutingCache().containsKey(serviceId)) {
            String routingKey = routingCacheService.getRoutingCache().get(serviceId);
            if(routingKey != null) {
                return routingKey;
            }
        }

        // at this point we did not find any entries in the exclusive cache. so at least one call to redis will have
        // to be made. in order to increase performance, we fetch exclusive and default routing values in one pipelined call
        Response<String> exclusiveRoutingKeyResponse;
        Response<String> defaultRoutingKeyResponse;
        try(Jedis jedis = jedisReadPool.getResource()) {
            Pipeline pipelined = jedis.pipelined();

            exclusiveRoutingKeyResponse = pipelined.get("rpc_map_exclusive:" + serviceId);
            defaultRoutingKeyResponse = pipelined.get("rpc_map_v2:" + serviceId);

            pipelined.sync();
        }
        String exclusiveRoutingKey = exclusiveRoutingKeyResponse.get();
        String defaultRoutingKey = defaultRoutingKeyResponse.get();

        if (!StringUtils.isEmpty(exclusiveRoutingKey)) {
            routingCacheService.addToExclusiveRoutingCache(serviceId, exclusiveRoutingKey);
            return exclusiveRoutingKey;
        } else {
            routingCacheService.addToExclusiveRoutingCache(serviceId, null);
        }

        if (routingCacheService.getRoutingCache().containsKey(serviceId)) {
            return routingCacheService.getRoutingCache().get(serviceId);
        }

        if (!StringUtils.isEmpty(defaultRoutingKey)) {
            routingCacheService.addToRoutingCache(serviceId, defaultRoutingKey);
            return defaultRoutingKey;
        }

        LOG.error("Routing key is empty for serviceId {} please verify your redis db config!", serviceId);
        throw new RuntimeException();
    }

    public <T> CacheResult<T> findInCache(String methodName, String serviceId, Param... params) {
        long rttStart = System.currentTimeMillis();
        RpcCacheResult cacheResult = cacheService.getCacheResult(serviceId, methodName, params);
        if (Boolean.TRUE.equals(cacheResult.wasExecuted())) {
            if (Boolean.TRUE.equals(cacheResult.wasFound())) {
                this.meterRegistry.counter("rpc.rabbit.cache.hit", "service", serviceId, "method", methodName).increment();
                this.meterRegistry.timer("rpc.rabbit.cache.duration", "service", serviceId, "method", methodName).record(System.currentTimeMillis() - rttStart, TimeUnit.MILLISECONDS);
                return new CacheResult<>(true, (T) cacheResult.getCacheResult());
            } else {
                this.meterRegistry.counter("rpc.rabbit.cache.miss", "service", serviceId, "method", methodName).increment();
            }
        }
        return new CacheResult<>(false);
    }

    public User getUser() {
        if(SecurityContextHolder.getContext().getAuthentication() != null) {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof User) {
                return (User) principal;
            }
        }

        return null;
    }

    public UUID getLoggedInUserId() {
        User user = getUser();
        return user != null ? user.getId() : null;
    }

    public String getHostName() {
        String hostname = null;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            // noop
        }

        return hostname;
    }

    private Timings getTimings(Message message, Long rpcOperationStartedAt) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();

        Object rttOneRaw = headers.get(Headers.RPC_RTT.name());
        Object invokedAtRaw = headers.get(Headers.RPC_INVOKED_AT.name());
        // skip all timing processing of other service gave us no data (happens when it fails with exceptions)
        if(rttOneRaw == null || invokedAtRaw == null) {
            return null;
        }

        long rttOne = (Long) rttOneRaw;
        long invokedAt = (Long) invokedAtRaw;
        long rttTwo = System.currentTimeMillis() - invokedAt;

        long rabbitRTT = rttOne + rttTwo;
        long totalOperationDuration = System.currentTimeMillis() - rpcOperationStartedAt;
        long methodExecutionTime = totalOperationDuration - rabbitRTT;

        Timings timings = new Timings();
        timings.setRabbitRTT(rabbitRTT);
        timings.setTotalOperationDuration(totalOperationDuration);
        timings.setMethodExecutionTime(methodExecutionTime);

        return timings;
    }

    public void trackStatistics(Message message, Long rpcOperationStartedAt, String serviceId, String methodName) {
        Timings timings = this.getTimings(message, rpcOperationStartedAt);
        if(timings != null) {
            LOG.debug("RPC command {}.{} - method execution time {}ms | rabbit RTT {}ms | total duration {}ms",
                    serviceId, methodName, timings.getMethodExecutionTime(), timings.getRabbitRTT(), timings.getTotalOperationDuration());

            this.meterRegistry.timer("rpc.calls", "service", serviceId, "method", methodName).record(timings.getMethodExecutionTime(), TimeUnit.MILLISECONDS);
            this.meterRegistry.timer("rpc.rabbit.rtt", "service", serviceId, "method", methodName).record(timings.getRabbitRTT(), TimeUnit.MILLISECONDS);
            this.meterRegistry.timer("rpc.rabbit.duration", "service", serviceId, "method", methodName).record(timings.getTotalOperationDuration(), TimeUnit.MILLISECONDS);
        }

        if (message.getMessageProperties().getHeaders().containsKey(Headers.PAYLOAD_SIZE.name())) {
            Integer payloadSize = (Integer) message.getMessageProperties().getHeaders().get(Headers.PAYLOAD_SIZE.name());
            this.meterRegistry.counter("rpc.rabbit.payload-size", "service", serviceId, "method", methodName).increment(payloadSize.doubleValue());
        }
    }

    /**
     * Currently this is used to map unknown response types. If a type could not be resolved, we will try to return a
     * LinkedHashMap or a String inside of a response envelope
     *
     * @param responsePayload
     * @return
     */
    public GenericResponse getGenericResponse(String responsePayload) {
        GenericResponse genericResponse = new GenericResponse();

        Object response;
        try {
            response = new Gson().fromJson(responsePayload, LinkedHashMap.class);
        } catch (JsonSyntaxException e) {
            response = new Gson().fromJson(responsePayload, String.class);
        }

        genericResponse.setResponse(response);
        genericResponse.setResponseType(response.getClass().getName());

        return genericResponse;
    }

    public List<String> getTypeHints(Message message) {
        if(message.getMessageProperties().getHeaders().containsKey(Headers.TYPE_HINTS.name())) {
            String hintHeader = (String) message.getMessageProperties().getHeaders().get(Headers.TYPE_HINTS.name());
            if(StringUtils.isNotEmpty(hintHeader)) {
                return Arrays.asList(hintHeader.split("\\|"));
            }
        }

        return Collections.emptyList();
    }

    public String decompress(Message message) {
        String responsePayload = new String(message.getBody());
        if (message.getMessageProperties().getHeaders().containsKey(Headers.IS_COMPRESSED.name()) &&
                Boolean.TRUE.equals(message.getMessageProperties().getHeaders().get(Headers.IS_COMPRESSED.name()))) {
            try {
                responsePayload = Base64GZipCompression.decompress(message.getBody());
            } catch (IOException e) {
                LOG.error("Failed to decompress payload " + responsePayload, e);
                return null;
            }
        }

        return responsePayload;
    }

    /**
     * name of the current microservice using the rpc framework
     */
    public String getServiceName() {
        return serviceName;
    }
}
