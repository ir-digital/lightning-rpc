package de.seepex.service;

public class CacheResult<T> {

    private boolean found;
    private T result;

    public CacheResult(boolean found, T result) {
        this.found = found;
        this.result = result;
    }

    public CacheResult(boolean found) {
        this.found = found;
    }

    public boolean isFound() {
        return found;
    }

    public T getResult() {
        return result;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public void setResult(T result) {
        this.result = result;
    }
}
