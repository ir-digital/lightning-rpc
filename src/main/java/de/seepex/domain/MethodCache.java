package de.seepex.domain;

import com.fasterxml.jackson.core.type.TypeReference;

public interface MethodCache {

    TypeReference getTypeReference();
    Long getExpireTimeSeconds();
    String name();

}
