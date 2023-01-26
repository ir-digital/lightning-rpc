package de.seepex.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import de.seepex.domain.MethodCache;
import de.seepex.domain.RegisteredCache;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.util.Pool;

import java.util.*;

@Component
public class SpxCacheManager {

    private final Pool<Jedis> jedisPool;
    private final Pool<Jedis> jedisReadPool;
    private final ObjectMapper msgPackObjectMapper = new ObjectMapper(new MessagePackFactory());

    private static final String PREFIX = "SPX_CACHE";

    private static final Logger LOG = LoggerFactory.getLogger(SpxCacheManager.class);

    @Autowired
    public SpxCacheManager(@Qualifier("serviceDocWritePool") Pool<Jedis> jedisPool, @Qualifier("serviceDocReadPool") Pool<Jedis> jedisReadPool) {
        this.jedisPool = jedisPool;
        this.jedisReadPool = jedisReadPool;
    }

    public void set(Object key, Object value, String cacheName, int expirationTimeInSeconds) {
        try {
            String s = Base64.getEncoder().encodeToString(msgPackObjectMapper.writeValueAsBytes(value));
            try (Jedis jedis = jedisPool.getResource()) {
                String cacheKey = PREFIX + ":" + cacheName + ":" + key;
                cacheKey = cacheKey.toLowerCase();

                Pipeline pipelined = jedis.pipelined();
                pipelined.set(cacheKey, s);
                pipelined.expire(cacheKey, expirationTimeInSeconds);
                pipelined.sync();
            }

        } catch (JsonProcessingException e) {
            LOG.error("Failed to set cache entry", e);
        }
    }

    public void set(Object key, Object value, MethodCache cacheDefinition) {
        set(key, value, cacheDefinition.name(), cacheDefinition.getExpireTimeSeconds().intValue());
    }

    public <T extends CacheResult> T get(Object key, RegisteredCache registeredCache) {
        Response<String> result;
        String cacheKey;
        try (Jedis jedis = jedisReadPool.getResource()) {
            cacheKey = PREFIX + ":" + registeredCache.name() + ":" + key;
            cacheKey = cacheKey.toLowerCase();

            // we assume no hit if key not present or remaining ttl < 2 sec.

            Pipeline pipelined = jedis.pipelined();

            Response<Boolean> exists = pipelined.exists(cacheKey);
            Response<Long> ttl = pipelined.ttl(cacheKey);
            result = pipelined.get(cacheKey);

            pipelined.sync();

            if (!exists.get() || ttl.get() < 2) {
                return (T) new CacheResult(false);
            }

            if (result.get() == null) {
                return (T) new CacheResult(true, null);
            }
        }

        try {
            byte[] decode = Base64.getDecoder().decode(result.get());
            Object deserialized = null;

            if(registeredCache.getType() == RegisteredCache.CacheType.ELEMENT) {
                deserialized = msgPackObjectMapper.readValue(decode, registeredCache.getBaseType());
                return (T) new CacheResult<>(true, deserialized);
            }

            if(registeredCache.getType() == RegisteredCache.CacheType.MAP) {

                String content1 = registeredCache.getContentClassNames().get(0);
                String content2 = registeredCache.getContentClassNames().get(1);

                JavaType type = msgPackObjectMapper.getTypeFactory().constructMapLikeType(Map.class, Class.forName(content1), Class.forName(content2));
                deserialized = msgPackObjectMapper.readValue(decode, type);

                return (T) new CacheResult<>(true, deserialized);
            }

            if(registeredCache.getType() == RegisteredCache.CacheType.LIST) {
                JavaType type = msgPackObjectMapper.getTypeFactory().constructCollectionType(List.class, Class.forName(registeredCache.getContentClassNames().get(0)));
                deserialized = msgPackObjectMapper.readValue(decode, type);
            }

            if(registeredCache.getType() == RegisteredCache.CacheType.SET) {
                JavaType type = msgPackObjectMapper.getTypeFactory().constructCollectionType(Set.class, Class.forName(registeredCache.getContentClassNames().get(0)));
                deserialized = msgPackObjectMapper.readValue(decode, type);
            }

            return (T) new CacheResult<>(true, deserialized);
        } catch (Exception e) {
            LOG.error("Failed to get cache entry for cacheKey: [" + cacheKey + "] loaded content [" + result.get() + "]", e);
        }

        return (T) new CacheResult(false);
    }

    public <T extends CacheResult> T get(Object key, String cacheName, TypeReference typeReference) {
        Response<String> result;
        String cacheKey;
        try (Jedis jedis = jedisReadPool.getResource()) {
            cacheKey = PREFIX + ":" + cacheName + ":" + key;
            cacheKey = cacheKey.toLowerCase();

            // we assume no hit if key not present or remaining ttl < 2 sec.

            Pipeline pipelined = jedis.pipelined();

            Response<Boolean> exists = pipelined.exists(cacheKey);
            Response<Long> ttl = pipelined.ttl(cacheKey);
            result = pipelined.get(cacheKey);

            pipelined.sync();

            if (!exists.get() || ttl.get() < 2) {
                return (T) new CacheResult(false);
            }

            if (result.get() == null) {
                return (T) new CacheResult(true, null);
            }
        }

        try {
            byte[] decode = Base64.getDecoder().decode(result.get());
            Object deserialized = msgPackObjectMapper.readValue(decode, typeReference);
            return (T) new CacheResult<>(true, deserialized);
        } catch (Exception e) {
            LOG.error("Failed to get cache entry for cacheKey: [" + cacheKey + "] loaded content [" + result.get() + "]", e);
        }

        return (T) new CacheResult(false);
    }

    public <T extends CacheResult> T get(Object key, MethodCache cacheDefinition) {
        return get(key, cacheDefinition.name(), cacheDefinition.getTypeReference());
    }

    public void delete(Object key, String cacheName) {
        try (Jedis jedis = jedisPool.getResource()) {
            String cacheKey = PREFIX + ":" + cacheName + ":" + key;
            cacheKey = cacheKey.toLowerCase();
            jedis.del(cacheKey);
        }
    }

    public void delete(Object key, MethodCache cacheDefinition) {
        delete(key, cacheDefinition.name());
    }

    public void purge(MethodCache methodCache) {
        purge(methodCache.name());
    }

    public void purge(String cacheName) {
        try (Jedis jedis = jedisPool.getResource()) {

            String pattern = PREFIX + ":" + cacheName + ":*";
            Set<String> keys = jedis.keys(pattern.toLowerCase());

            Pipeline pipelined = jedis.pipelined();
            for (String key : keys) {
                pipelined.del(key);
            }
            pipelined.sync();
        }
    }

}
