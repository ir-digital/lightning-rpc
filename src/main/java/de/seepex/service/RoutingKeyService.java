package de.seepex.service;

import com.google.gson.Gson;
import de.seepex.domain.Routing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.util.Pool;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;


@Service
public class RoutingKeyService {

    private final Pool<Jedis> jedisPool;
    private final Pool<Jedis> jedisReadPool;
    private final Environment environment;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RoutingCacheService routingCacheService;

    private final Gson gson = new Gson();
    private static final Logger LOG = LoggerFactory.getLogger(RoutingKeyService.class);

    private String RPC_EXCLUSIVE_ROUTING_TOPIC = "rpc_exclusive_routing";
    private String RPC_ROUTING_TOPIC = "rpc_routing";

    public RoutingKeyService(@Qualifier("documentation-redis") RedisTemplate<String, Object> redisTemplate, @Qualifier("serviceDocWritePool") Pool<Jedis> jedisPool,
                             @Qualifier("serviceDocReadPool") Pool<Jedis> jedisReadPool, RoutingCacheService routingCacheService, Environment environment) {
        this.jedisPool = jedisPool;
        this.jedisReadPool = jedisReadPool;
        this.environment = environment;
        this.redisTemplate = redisTemplate;
        this.routingCacheService = routingCacheService;
    }

    @PostConstruct
    public void subscribeToRpcRouting() {
        String activeProfile = environment.getActiveProfiles()[0];

        // unknown profile naming
        if(!activeProfile.equalsIgnoreCase("dev") &&
                !activeProfile.equalsIgnoreCase("test") &&
                !activeProfile.equalsIgnoreCase("integrationtest") &&
                !activeProfile.equalsIgnoreCase("gcloud")) {
            RPC_EXCLUSIVE_ROUTING_TOPIC += "_" + activeProfile;
            RPC_ROUTING_TOPIC += "_" + activeProfile;
        } else if(!activeProfile.equalsIgnoreCase("gcloud")) {
            RPC_EXCLUSIVE_ROUTING_TOPIC += "_test";
            RPC_ROUTING_TOPIC += "_test";
        }

        JedisPubSub jedisPubSub = new JedisPubSub() {

            @Override
            public void onMessage(String channel, String message) {
                Routing routing = gson.fromJson(message, Routing.class);

                if(routing.getExclusive()) {
                    LOG.info("Received exclusive routing {}", routing);

                    if(!routing.getDelete()) {
                        redisTemplate.opsForValue().set("rpc_map_exclusive:" + routing.getServiceId(), routing.getRoutingKey(), 3, TimeUnit.MINUTES);
                        routingCacheService.getExclusiveRoutingCache().put(routing.getServiceId(), routing.getRoutingKey());
                    } else {
                        redisTemplate.delete("rpc_map_exclusive:" + routing.getServiceId());
                        routingCacheService.getExclusiveRoutingCache().remove(routing.getServiceId());
                    }
                } else {
                    redisTemplate.opsForValue().set("rpc_map_v2:" + routing.getServiceId(), routing.getRoutingKey(), 1, TimeUnit.HOURS);
                    routingCacheService.getRoutingCache().put(routing.getServiceId(), routing.getRoutingKey());
                }
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                LOG.info("Client is Subscribed to channel : {}. Subscribed to total {} channels", channel, subscribedChannels);
            }

            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                LOG.info("Client is Unsubscribed to channel : {}. Subscribed to total {} channels", channel, subscribedChannels);
            }
        };

        new Thread(() -> {
            try (Jedis jedis = jedisReadPool.getResource()){
                jedis.subscribe(jedisPubSub, RPC_EXCLUSIVE_ROUTING_TOPIC);
                jedis.subscribe(jedisPubSub, RPC_ROUTING_TOPIC);
            } 
        }).start();
    }

    /**
     * Notify all other consumers about a new exclusive routing
     */
    public void publish(Routing routing) {
        try(Jedis jedis = jedisPool.getResource()) {
            if(routing.getExclusive()) {
                jedis.publish(RPC_EXCLUSIVE_ROUTING_TOPIC, gson.toJson(routing));
            } else {
                jedis.publish(RPC_ROUTING_TOPIC, gson.toJson(routing));
            }
        }
    }
}
