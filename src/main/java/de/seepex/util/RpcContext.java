package de.seepex.util;

import de.seepex.domain.RpcContextPayload;

import java.util.HashMap;
import java.util.Map;

public class RpcContext {

    private static final ThreadLocal<RpcContextPayload> CONTEXT = ThreadLocal.withInitial(RpcContextPayload::new);

    public static void setUserId(String userId) {
        RpcContextPayload rpcContext = CONTEXT.get();
        rpcContext.setUserId(userId);
        rpcContext.setInitialized(true);

        CONTEXT.set(rpcContext);
    }

    public static void setCallerClass(String callerClass) {
        RpcContextPayload rpcContext = CONTEXT.get();
        rpcContext.setCallerClass(callerClass);
        rpcContext.setInitialized(true);

        CONTEXT.set(rpcContext);
    }

    public static void setHostname(String hostname) {
        RpcContextPayload rpcContext = CONTEXT.get();
        rpcContext.setHostname(hostname);
        rpcContext.setInitialized(true);

        CONTEXT.set(rpcContext);
    }

    public static void setApplicationHeaders(Map<String, String> applicationHeaders) {
        RpcContextPayload rpcContext = CONTEXT.get();
        rpcContext.setApplicationHeaders(applicationHeaders);
        rpcContext.setInitialized(true);

        CONTEXT.set(rpcContext);
    }

    public static String getUserId() {
        return CONTEXT.get().getUserId();
    }

    public static String getCallerClass() {
        return CONTEXT.get().getCallerClass();
    }

    public static String getHostname() {
        return CONTEXT.get().getHostname();
    }

    public static boolean isInitialized() {
        return CONTEXT.get().isInitialized();
    }

    public static Map<String, String> getApplicationHeaders() {
        Map<String, String> applicationHeaders = CONTEXT.get().getApplicationHeaders();
        if(applicationHeaders == null) {
            applicationHeaders = new HashMap<>();
        }

        return applicationHeaders;
    }

    public static String getAsString() {
        return "{initialized: " + isInitialized() + ", userId: " + getUserId() +
            ", callerClass:" + getCallerClass() + ", caller hostname: " + getHostname() + ", applicationHeaders: " + getApplicationHeaders() + "}";
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
