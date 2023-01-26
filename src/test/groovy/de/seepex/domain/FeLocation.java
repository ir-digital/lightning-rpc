package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeLocation extends FeLocationSimple {

    private List<FeDeviceSimple> devices = new ArrayList<>();
    private List<FeLocationSimple> children = new ArrayList<>();

    public List<FeDeviceSimple> getDevices() {
        if(devices == null) {
            this.devices = new ArrayList<>();
        }
        return devices;
    }

    public void setDevices(List<FeDeviceSimple> devices) {
        this.devices = devices;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public FeLocationSimple getParent() {
        return parent;
    }

    public void setParent(FeLocationSimple parent) {
        this.parent = parent;
    }

    public List<FeLocationSimple> getChildren() {
        if(children == null) {
            this.children = new ArrayList<>();
        }
        return children;
    }

    public void setChildren(List<FeLocationSimple> children) {
        this.children = children;
    }

}
