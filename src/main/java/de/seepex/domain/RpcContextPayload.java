package de.seepex.domain;

import java.util.HashMap;
import java.util.Map;

public class RpcContextPayload {

    private String userId;
    private String callerClass;
    private String hostname;
    private Map<String, String> applicationHeaders = new HashMap<>();
    private boolean initialized = false;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCallerClass() {
        return callerClass;
    }

    public void setCallerClass(String callerClass) {
        this.callerClass = callerClass;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public Map<String, String> getApplicationHeaders() {
        return applicationHeaders;
    }

    public void setApplicationHeaders(Map<String, String> applicationHeaders) {
        this.applicationHeaders = applicationHeaders;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public String toString() {
        return "RpcContextPayload{" +
                "userId='" + userId + '\'' +
                ", callerClass='" + callerClass + '\'' +
                ", hostname='" + hostname + '\'' +
                ", applicationHeaders=" + applicationHeaders +
                ", initialized=" + initialized +
                '}';
    }
}
