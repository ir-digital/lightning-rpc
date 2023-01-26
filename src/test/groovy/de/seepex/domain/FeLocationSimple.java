package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeLocationSimple {

    protected UUID id;
    protected String name;
    protected String description;
    protected FeLocationSimple parent;
    protected List<UUID> groupsV2;
    protected List<UUID> usersV2;
    protected long deviceCount;
    protected long subLocationCount;

    public List<UUID> getGroupsV2() {
        return groupsV2;
    }

    public void setGroupsV2(List<UUID> groupsV2) {
        this.groupsV2 = groupsV2;
    }

    public List<UUID> getUsersV2() {
        return usersV2;
    }

    public void setUsersV2(List<UUID> usersV2) {
        this.usersV2 = usersV2;
    }

    public long getSubLocationCount() {
        return subLocationCount;
    }

    public void setSubLocationCount(long subLocationCount) {
        this.subLocationCount = subLocationCount;
    }

    public long getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(long deviceCount) {
        this.deviceCount = deviceCount;
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
    
}
