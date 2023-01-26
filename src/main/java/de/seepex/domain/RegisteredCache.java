package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;

import java.util.List;

public class RegisteredCache implements MethodCache {

    @JsonIgnore
    private TypeReference typeReference;

    private Long expirationTimeInSeconds;
    private String name;

    private String service;
    private String method;

    @JsonIgnore
    private JavaType baseType;

    private String baseClassName;
    private List<String> contentClassNames;
    private CacheType type;

    public enum CacheType {
        MAP, LIST, SET, ELEMENT
    }

    public RegisteredCache(String name, TypeReference typeReference, Long expirationTimeInSeconds) {
        this.typeReference = typeReference;
        this.expirationTimeInSeconds = expirationTimeInSeconds;
        this.name = name;
    }

    @Override
    public TypeReference getTypeReference() {
        return typeReference;
    }

    @Override
    public Long getExpireTimeSeconds() {
        return expirationTimeInSeconds;
    }

    @Override
    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public JavaType getBaseType() {
        return baseType;
    }

    public void setBaseType(JavaType baseType) {
        this.baseType = baseType;
    }

    public String getBaseClassName() {
        return baseClassName;
    }

    public void setBaseClassName(String baseClassName) {
        this.baseClassName = baseClassName;
    }

    public List<String> getContentClassNames() {
        return contentClassNames;
    }

    public void setContentClassNames(List<String> contentClassNames) {
        this.contentClassNames = contentClassNames;
    }

    public CacheType getType() {
        return type;
    }

    public void setType(CacheType type) {
        this.type = type;
    }

    public void setTypeReference(TypeReference typeReference) {
        this.typeReference = typeReference;
    }

    public Long getExpirationTimeInSeconds() {
        return expirationTimeInSeconds;
    }

    public void setExpirationTimeInSeconds(Long expirationTimeInSeconds) {
        this.expirationTimeInSeconds = expirationTimeInSeconds;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String toString() {
        return "RegisteredCache{" +
                "typeReference=" + typeReference +
                ", expirationTimeInSeconds=" + expirationTimeInSeconds +
                ", name='" + name + '\'' +
                ", service='" + service + '\'' +
                ", method='" + method + '\'' +
                ", baseType=" + baseType +
                ", baseClassName='" + baseClassName + '\'' +
                ", contentClassNames=" + contentClassNames +
                ", type=" + type +
                '}';
    }
}
