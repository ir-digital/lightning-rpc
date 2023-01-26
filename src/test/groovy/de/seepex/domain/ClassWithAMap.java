package de.seepex.domain;

import java.util.Map;

public class ClassWithAMap {

    private String name;

    private Map<String, String> metadata;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
}
