package de.seepex.domain;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public enum TestCacheDefinitions implements MethodCache {

    SOME_SENSOR_CACHE(new TypeReference<Sensor>(){}, 10, TimeUnit.MINUTES),
    SOME_ALARM_CACHE(new TypeReference<List<Alarm>>(){}, 10, TimeUnit.MINUTES);

    private TypeReference typeReference;
    private Long expireTimeSeconds;

    TestCacheDefinitions(TypeReference c, Integer expireTime, TimeUnit expireTimeUnit) {
        this.typeReference = c;
        this.expireTimeSeconds = expireTimeUnit.toSeconds(expireTime);
    }

    public TypeReference getTypeReference() {
        return typeReference;
    }

    public Long getExpireTimeSeconds() {
        return expireTimeSeconds;
    }
}
