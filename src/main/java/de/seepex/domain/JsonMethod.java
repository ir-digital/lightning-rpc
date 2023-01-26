package de.seepex.domain;

import java.util.List;

public class JsonMethod {

    private String name;
    private String description;
    private List<JsonParameter> parameters;
    private ReturnValue returnValue;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<JsonParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<JsonParameter> parameters) {
        this.parameters = parameters;
    }

    public ReturnValue getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(ReturnValue returnValue) {
        this.returnValue = returnValue;
    }
}
