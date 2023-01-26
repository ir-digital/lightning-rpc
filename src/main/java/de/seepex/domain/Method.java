package de.seepex.domain;

import java.lang.reflect.Parameter;
import java.util.List;

public class Method {

    private String name;
    private String description;
    private List<Parameter> parameters;
    private ReturnValue returnValue;
    private MethodCache cacheDefinition;

    public MethodCache getCacheDefinition() {
        return cacheDefinition;
    }

    public void setCacheDefinition(MethodCache cacheDefinition) {
        this.cacheDefinition = cacheDefinition;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ReturnValue getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(ReturnValue returnValue) {
        this.returnValue = returnValue;
    }
}
