package de.seepex.domain;

import java.util.List;

public class JsonSpxClass {

    private String id;
    private String name;
    private String description;
    List<JsonMethod> methods;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public List<JsonMethod> getMethods() {
        return methods;
    }

    public void setMethods(List<JsonMethod> methods) {
        this.methods = methods;
    }
}
