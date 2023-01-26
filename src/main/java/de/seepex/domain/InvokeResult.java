package de.seepex.domain;

import java.util.List;

public class InvokeResult {

    private Object response;
    private java.lang.reflect.Method method;
    private List<String> responseTypeHints;
    private boolean failed = false;
    private String exceptionText;
    private String exceptionClassName;

    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public String getExceptionText() {
        return exceptionText;
    }

    public void setExceptionText(String exceptionText) {
        this.exceptionText = exceptionText;
    }

    public String getExceptionClassName() {
        return exceptionClassName;
    }

    public void setExceptionClassName(String exceptionClassName) {
        this.exceptionClassName = exceptionClassName;
    }

    public void setResponse(Object response) {
        this.response = response;
    }

    public Object getResponse() {
        return response;
    }

    public java.lang.reflect.Method getMethod() {
        return method;
    }

    public void setMethod(java.lang.reflect.Method method) {
        this.method = method;
    }

    public List<String> getResponseTypeHints() {
        return responseTypeHints;
    }

    public void setResponseTypeHints(List<String> responseTypeHints) {
        this.responseTypeHints = responseTypeHints;
    }
}
