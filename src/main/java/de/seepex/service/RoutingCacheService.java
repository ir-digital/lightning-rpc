package de.seepex.service;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
public class RoutingCacheService {

    private PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<String, String> expirePeriod = new PassiveExpiringMap.ConstantTimeToLiveExpirationPolicy<>(10, TimeUnit.MINUTES);
    private PassiveExpiringMap<String, String> routingCache = new PassiveExpiringMap<>(expirePeriod, new HashMap<>());
    private PassiveExpiringMap<String, String> exclusiveRoutingCache = new PassiveExpiringMap<>(expirePeriod, new HashMap<>());

    public PassiveExpiringMap<String, String> getRoutingCache() {
        return routingCache;
    }

    public PassiveExpiringMap<String, String> getExclusiveRoutingCache() {
        return exclusiveRoutingCache;
    }

    public void addToRoutingCache(String key, String value) {
        routingCache.put(key, value);
    }

    public void addToExclusiveRoutingCache(String key, String value) {
        exclusiveRoutingCache.put(key, value);
    }
}
