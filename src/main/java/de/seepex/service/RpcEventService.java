package de.seepex.service;

import com.google.gson.Gson;
import de.seepex.domain.RpcEvent;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.util.Pool;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RpcEventService {

    private final Pool<Jedis> jedisPool;
    private final Pool<Jedis> jedisReadPool;
    private final Environment environment;
    private final RpcTools rpcTools;

    private Map<String, RpcEvent> rpcEventCache = Collections.synchronizedMap(new PassiveExpiringMap(new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(3, TimeUnit.MINUTES), new HashMap<>()));

    private final Gson gson = new Gson();
    private String EVENT_QUEUE = "rpc_call_events";

    private static final Logger LOG = LoggerFactory.getLogger(RpcEventService.class);

    public RpcEventService(@Qualifier("serviceDocWritePool") Pool<Jedis> jedisPool, @Qualifier("serviceDocReadPool") Pool<Jedis> jedisReadPool,
                           Environment environment, RpcTools rpcTools) {
        this.jedisPool = jedisPool;
        this.jedisReadPool = jedisReadPool;
        this.environment = environment;
        this.rpcTools = rpcTools;
    }
    
    @PostConstruct
    public void subscribeToRpcEventRouting() {
        String activeProfile = environment.getActiveProfiles()[0];

        // unknown profile naming
        if(!activeProfile.equalsIgnoreCase("dev") &&
                !activeProfile.equalsIgnoreCase("test") &&
                !activeProfile.equalsIgnoreCase("integrationtest") &&
                !activeProfile.equalsIgnoreCase("gcloud")) {
            EVENT_QUEUE += "_" + activeProfile;
        } else if(!activeProfile.equalsIgnoreCase("gcloud")) {
            EVENT_QUEUE += "_test";
        }

        JedisPubSub jedisPubSub = new JedisPubSub() {

            @Override
            public void onMessage(String channel, String message) {
                RpcEvent rpcEvent = gson.fromJson(message, RpcEvent.class);
                rpcEventCache.put(rpcEvent.getCorrelationId(), rpcEvent);

                // this is required to trigger removal of expired entries
                int cacheSize = rpcEventCache.size();
                LOG.debug("rpcEvent cache size {}", cacheSize);
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

        // subscribe to the queue of the own service, as here we are only interested in events sent to
        // the queue of the own service
        String subscribtionQueue = EVENT_QUEUE + "." + rpcTools.getServiceName();

        new Thread(() -> {
            try (Jedis jedis = jedisReadPool.getResource()){
                jedis.subscribe(jedisPubSub, subscribtionQueue);
            }
        }).start();
    }

    public RpcEvent getEvent(String correlationId) {
        return rpcEventCache.get(correlationId);
    }

    public void rpcCallReceived(String correlationId, String callerService) {
        // cannot track calls without id
        if(correlationId == null) {
            return;
        }

        RpcEvent event = new RpcEvent();
        event.setCorrelationId(correlationId);
        event.setHostname(rpcTools.getHostName());
        event.setTimestamp(Instant.now().getEpochSecond());
        event.setType(RpcEvent.RpcEventType.RECEIVE_CALL);

        String responseQueue = EVENT_QUEUE + "." + callerService;

        Runnable announcer =
                () -> {
                    try(Jedis jedis = jedisPool.getResource()) {
                        jedis.publish(responseQueue, gson.toJson(event));
                    }
                };

        announcer.run();
    }
}
