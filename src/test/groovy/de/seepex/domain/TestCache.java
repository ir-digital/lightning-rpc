package de.seepex.domain;

import com.fasterxml.jackson.core.type.TypeReference;
import de.seepex.DeviceForTest;

import java.util.concurrent.TimeUnit;

public enum TestCache implements MethodCache {

    FAKE_DEVICE_CACHE(new TypeReference<DeviceForTest>(){}, 5, TimeUnit.MINUTES);

    private TypeReference typeReference;
    private Long expireTimeSeconds;

    TestCache(TypeReference c, Integer expireTime, TimeUnit expireTimeUnit) {
        this.typeReference = c;
        this.expireTimeSeconds = expireTimeUnit.toSeconds(expireTime);
    }

    public TypeReference getTypeReference() {
        return typeReference;
    }

    public Long getExpireTimeSeconds() {
        return expireTimeSeconds;
    }

    public static class Constants {
        public static final String FAKE_DEVICE_CACHE = "FAKE_DEVICE_CACHE";
    }

}
