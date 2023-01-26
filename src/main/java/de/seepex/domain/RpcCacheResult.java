package de.seepex.domain;

public class RpcCacheResult {

    private Boolean wasExecuted;
    private Boolean wasFound;
    private Object cacheResult;

    public RpcCacheResult(Boolean wasExecuted, Boolean wasFound, Object cacheResult) {
        this.wasExecuted = wasExecuted;
        this.wasFound = wasFound;
        this.cacheResult = cacheResult;
    }

    public Boolean wasFound() {
        return wasFound;
    }

    public Boolean wasExecuted() {
        return wasExecuted;
    }

    public Object getCacheResult() {
        return cacheResult;
    }

    public void setCacheResult(Object cacheResult) {
        this.cacheResult = cacheResult;
    }
}
