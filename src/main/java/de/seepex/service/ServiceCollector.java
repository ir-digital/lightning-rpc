package de.seepex.service;

import de.seepex.annotation.ProviderType;
import de.seepex.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ServiceCollector {

    private HashMap<SpxClass, List<Method>> classMap = new HashMap<>();
    private HashMap<String, String> routingMap = new HashMap<>();
    private HashMap<String, String> exclusiveRoutingMap = new HashMap<>();

    private final RedisTemplate<String, Object> redisTemplate;
    private final RoutingKeyService routingKeyService;
    private final RoutingCacheService routingCacheService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${register-routing:false}")
    private Boolean registerRouting;

    @Autowired
    public ServiceCollector(@Qualifier("documentation-redis") RedisTemplate<String, Object> redisTemplate,
                            RoutingKeyService routingKeyService, RoutingCacheService routingCacheService) {
        this.redisTemplate = redisTemplate;
        this.routingKeyService = routingKeyService;
        this.routingCacheService = routingCacheService;
    }
    
    public void addMethod(SpxClass spxClass, String methodName, String description, MethodCache cacheDefinition,
                          List<Parameter> parameters, ReturnValue returnValue) {
        List<Method> methods = new ArrayList<>();

        for(SpxClass clazz : classMap.keySet()) {
            if(clazz.getId().equalsIgnoreCase(spxClass.getId())) {
                spxClass = clazz;
                methods = classMap.get(clazz);
            }
        }

        Method method = new Method();
        method.setName(methodName);
        method.setDescription(description);
        method.setParameters(parameters);
        method.setReturnValue(returnValue);
        method.setCacheDefinition(cacheDefinition);

        methods.add(method);

        classMap.put(spxClass, methods);
    }

    public List<SpxClass> getClasses() {
        ArrayList<SpxClass> spxClasses = new ArrayList<>(classMap.keySet());
        spxClasses.sort(Comparator.comparing(SpxClass::getName));

        return spxClasses;
    }

    public List<Method> getMethods(String classId) {
        if(classId == null) {
            return new ArrayList<>();
        }

        for(SpxClass clazz : classMap.keySet()) {
            if(clazz.getId().equalsIgnoreCase(classId)) {
                List<Method> methods = classMap.get(clazz);
                methods.sort(Comparator.comparing(Method::getName));
                
                return methods;
            }
        }

        return new ArrayList<>();
    }

    /**
     * Registers routing in cache / redis
     * If providerType == ProviderType.EXCLUSIVE it means that we want to route all messages
     * for a specific Service class to a dedicated queue (DEBUG, TESTING) so just one service will receive
     * all RPC commands.
     * In this case there is a extended logic - this exclusive routing is stored in a separate routing table / local cache
     * which is prioritized over the default routing. This routing will be removed when the application shuts down, so the
     * default behaviour is restored, once the EXCLUSIVE instance has been shut down.
     *
     * @param spxClass
     * @param routingKey
     */
    public void registerRoutingKey(SpxClass spxClass, String routingKey, ProviderType providerType) {
        if(routingKey == null) {
            return;
        }

        if(providerType.equals(ProviderType.EXCLUSIVE)) {
            if(Boolean.FALSE.equals(registerRouting)) {
                logger.warn("SpxClass {} has EXCLUSIVE ProviderType but registerRouting is disabled - make sure this is the intended setup" +
                        "as your local instance will not register the exclusive routing and wont receive any RPC requests", spxClass.getName());
            }
            logger.info("Adding to EXCLUSIVE routing map: service {} with routingKey {}", spxClass.getId(), routingKey);
            exclusiveRoutingMap.put(spxClass.getId(), routingKey + "_exclusive");
        } else {
            logger.info("Adding to SHARED routing map: service {} with routingKey {}", spxClass.getId(), routingKey);
            routingMap.put(spxClass.getId(), routingKey);
        }

        updateRoutingKeyMappings();
        updateExclusiveRoutingKeyMappings();
    }

    public void updateRoutingKeyMappings() {
        for(Map.Entry<String, String> entry : routingMap.entrySet()) {
            if(registerRouting) {
                Routing routing = new Routing();
                routing.setServiceId(entry.getKey());
                routing.setRoutingKey(entry.getValue());
                routing.setExclusive(false);

                routingKeyService.publish(routing);

                redisTemplate.opsForValue().set("rpc_map_v2:" + routing.getServiceId(), routing.getRoutingKey(), 1, TimeUnit.HOURS);
                routingCacheService.getRoutingCache().put(routing.getServiceId(), routing.getRoutingKey());

                logger.debug("Registering service V2 {} with routingKey {} successful", entry.getKey(), entry.getValue());
            }
        }
    }

    public void updateExclusiveRoutingKeyMappings() {
        for(Map.Entry<String, String> entry : exclusiveRoutingMap.entrySet()) {
            if(Boolean.TRUE.equals(registerRouting)) {
                Routing routing = new Routing();
                routing.setServiceId(entry.getKey());
                routing.setRoutingKey(entry.getValue());
                routing.setExclusive(true);
                routing.setDelete(false);

                routingKeyService.publish(routing);

                redisTemplate.opsForValue().set("rpc_map_exclusive:" + routing.getServiceId(), routing.getRoutingKey(), 3, TimeUnit.MINUTES);
                routingCacheService.getExclusiveRoutingCache().put(routing.getServiceId(), routing.getRoutingKey());
                logger.info("Registering service V2 {} with routingKey {} successful", routing.getServiceId(), routing.getRoutingKey());
            }
        }
    }

    @PreDestroy
    public void onExit() {
        for(Map.Entry<String, String> entry : exclusiveRoutingMap.entrySet()) {
            Routing routing = new Routing();
            routing.setServiceId(entry.getKey());
            routing.setExclusive(true);
            routing.setDelete(true);

            routingKeyService.publish(routing);
            redisTemplate.delete("rpc_map_exclusive:" + entry.getKey());
        }
    }
}
