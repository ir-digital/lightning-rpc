package de.seepex.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import de.seepex.domain.MetaReturn;
import de.seepex.domain.RegisteredCache;
import de.seepex.util.InvokePropertiesExtractor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.util.Pool;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CacheContainer {

    private final Map<String, RegisteredCache> caches = new ConcurrentHashMap<>();
    private final Map<String, RegisteredCache> cachesByServiceMethod = new ConcurrentHashMap<>();

    private final Pool<Jedis> jedisPool;
    private final Gson gson = new Gson();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static String TOPIC = "CACHE_REGISTRATIONS_";

    private static final Logger LOG = LoggerFactory.getLogger(CacheContainer.class);

    public CacheContainer(@Qualifier("serviceDocWritePool") Pool<Jedis> jedisPool, Environment environment) {
        this.jedisPool = jedisPool;

        String activeProfile = environment.getActiveProfiles()[0];
        TOPIC += activeProfile;
    }

    public String getKey(String serviceName, String methodName) {
        return serviceName + "|" + methodName;
    }

    private RegisteredCache getCacheDefinition(String message) {
        RegisteredCache registeredCache = gson.fromJson(message, RegisteredCache.class);

        TypeReference typeReference = registeredCache.getTypeReference();
        String className;
        if(typeReference != null) {
            className = registeredCache.getTypeReference().getType().getTypeName();
        } else {
            className = registeredCache.getBaseClassName();
        }

        final MetaReturn metaReturn = InvokePropertiesExtractor.getReturnClass(className);
        final Class returnClass = metaReturn.getReturnedBaseClass();

        // that means the returned class could not be mapped to a known entity.
        if(returnClass == null) {
            LOG.error("Could not map cache to a known object. {}", registeredCache);
            return null;
        }

        if (InvokePropertiesExtractor.isElement(returnClass)) {
            JavaType javaType = objectMapper.getTypeFactory().constructType(metaReturn.getReturnedBaseClass());

            registeredCache.setBaseType(javaType);
            registeredCache.setType(RegisteredCache.CacheType.ELEMENT);
        }

        if (InvokePropertiesExtractor.isList(returnClass)) {
            JavaType javaType = objectMapper.getTypeFactory().constructType(metaReturn.getReturnedBaseClass());

            registeredCache.setBaseType(javaType);
            registeredCache.setType(RegisteredCache.CacheType.LIST);
        }

        if (InvokePropertiesExtractor.isSet(returnClass)) {
            JavaType javaType = objectMapper.getTypeFactory().constructType(metaReturn.getReturnedBaseClass());

            registeredCache.setBaseType(javaType);
            registeredCache.setType(RegisteredCache.CacheType.SET);
        }

        if (InvokePropertiesExtractor.isMap(returnClass)) {
            JavaType javaType = objectMapper.getTypeFactory().constructType(metaReturn.getReturnedBaseClass());

            registeredCache.setBaseType(javaType);
            registeredCache.setType(RegisteredCache.CacheType.MAP);
        }

        return registeredCache;
    }

    @PostConstruct
    public void subscribeToCacheUpdates() {
        JedisPubSub jedisPubSub = new JedisPubSub() {

            @Override
            public void onMessage(String channel, String message) {
                LOG.debug("received cache message {}", message);
                RegisteredCache cacheDefinition = getCacheDefinition(message);
                if(cacheDefinition == null) {
                    return;
                }
                caches.put(cacheDefinition.name(), cacheDefinition);

                String serviceMethodKey = getKey(cacheDefinition.getService(), cacheDefinition.getMethod());
                cachesByServiceMethod.put(serviceMethodKey, cacheDefinition);
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
            }
        };

        new Thread(() -> {
            try (Jedis jedis = jedisPool.getResource()){
                jedis.subscribe(jedisPubSub, TOPIC);
            }
        }).start();
    }

    public void register(String name, RegisteredCache registeredCache) {
        String typeName = registeredCache.getTypeReference().getType().getTypeName();

        final MetaReturn metaReturn = InvokePropertiesExtractor.getReturnClass(typeName);
        final Class returnClass = metaReturn.getReturnedBaseClass();

        if (InvokePropertiesExtractor.isElement(returnClass)) {
            JavaType javaType = objectMapper.getTypeFactory().constructType(metaReturn.getReturnedBaseClass());

            registeredCache.setBaseType(javaType);
            registeredCache.setBaseClassName(typeName);
            registeredCache.setType(RegisteredCache.CacheType.ELEMENT);
        }

        if (InvokePropertiesExtractor.isList(returnClass)) {
            JavaType javaType = objectMapper.getTypeFactory().constructType(metaReturn.getReturnedBaseClass());

            registeredCache.setBaseType(javaType);
            registeredCache.setBaseClassName(List.class.getName());
            registeredCache.setContentClassNames(Collections.singletonList(metaReturn.getGenerics().get(0).getName()));
            registeredCache.setType(RegisteredCache.CacheType.LIST);
        }

        if (InvokePropertiesExtractor.isSet(returnClass)) {
            JavaType javaType = objectMapper.getTypeFactory().constructType(metaReturn.getReturnedBaseClass());

            registeredCache.setBaseType(javaType);
            registeredCache.setBaseClassName(Set.class.getName());
            registeredCache.setContentClassNames(Collections.singletonList(metaReturn.getGenerics().get(0).getName()));
            registeredCache.setType(RegisteredCache.CacheType.SET);
        }

        if (InvokePropertiesExtractor.isMap(returnClass)) {
            JavaType javaType = objectMapper.getTypeFactory().constructType(metaReturn.getReturnedBaseClass());

            registeredCache.setBaseType(javaType);
            registeredCache.setBaseClassName(Map.class.getName());
            registeredCache.setContentClassNames(Arrays.asList(metaReturn.getGenerics().get(0).getName(), metaReturn.getGenerics().get(1).getName()));
            registeredCache.setType(RegisteredCache.CacheType.MAP);
        }

        caches.put(name, registeredCache);
        publish(registeredCache);
    }

    public RegisteredCache get(String key) {
        return caches.get(key);
    }

    public RegisteredCache get(String service, String method) {
        String key = getKey(service, method);
        return cachesByServiceMethod.get(key);
    }

    public void announceCaches() {
        for(RegisteredCache cache: caches.values()) {
            publish(cache);
        }
    }

    private void publish(RegisteredCache registeredCache) {
        if(StringUtils.isEmpty(registeredCache.getMethod()) || StringUtils.isEmpty(registeredCache.getService())) {
            return;
        }

        try(Jedis jedis = jedisPool.getResource()) {
            jedis.publish(TOPIC, objectMapper.writeValueAsString(registeredCache));
        } catch (Exception e){
            LOG.error("Failed to publish", e);
        }
    }
}
