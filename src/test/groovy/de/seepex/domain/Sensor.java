package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Sensor  {

    private static final long serialVersionUID = 101L;

    private UUID id;
    private String name;
    private String location;
    private String unit;
    private String notes;
    private Map<String, String> metadata;

    @JsonAlias({"attachedToDevices", "attached_to_devices"})
    private List<UUID> attachedToDevices;

    @JsonAlias({"groupsV2", "groups_v2"})
    private List<UUID> groupsV2;

    @JsonAlias({"usersV2", "users_v2"})
    private List<UUID> usersV2;

    private UUID type;

    private String mappedUnit;

    @JsonAlias({"lastActiveNanos", "last_active_nanos"})
    private Long lastActiveNanos;

    @JsonAlias({"tenantId", "tenant_id"})
    private UUID tenantId;

    private Boolean deprecated;

    public Boolean isDeprecated() {
        return deprecated != null ? deprecated : Boolean.FALSE;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public List<UUID> getUsersV2() {
        if(usersV2 == null) {
            usersV2 = new ArrayList<>();
        }
        return usersV2;
    }

    public void setUsersV2(List<UUID> usersV2) {
        this.usersV2 = usersV2;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    public Long getLastActiveNanos() {
        return lastActiveNanos;
    }

    public void setLastActiveNanos(Long lastActiveNanos) {
        this.lastActiveNanos = lastActiveNanos;
    }

    public List<UUID> getGroupsV2() {
        return groupsV2;
    }

    public void setGroupsV2(List<UUID> groupsV2) {
        this.groupsV2 = groupsV2;
    }

    public UUID getType() {
        return type;
    }

    public void setType(UUID type) {
        this.type = type;
    }

    public List<UUID> getAttachedToDevices() {
        if(attachedToDevices == null) {
            this.attachedToDevices = new ArrayList<>();
        }
        return attachedToDevices;
    }

    public void setAttachedToDevices(List<UUID> attachedToDevices) {
        this.attachedToDevices = attachedToDevices;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        if(name == null){
            return "";
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getMappedUnit() {
        return mappedUnit;
    }

    public void setMappedUnit(String mappedUnit) {
        this.mappedUnit = mappedUnit;
    }

    public String getMappedOrRawUnit() {
        return mappedUnit != null ? mappedUnit : unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Sensor sensor = (Sensor) o;
        return Objects.equals(getId(), sensor.getId()) &&
                Objects.equals(getName(), sensor.getName()) &&
                Objects.equals(getUnit(), sensor.getUnit()) &&
                Objects.equals(getNotes(), sensor.getNotes()) &&
                Objects.equals(getMetadata(), sensor.getMetadata());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getUnit(), getNotes(), getMetadata());
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", unit='" + unit + '\'' +
                ", notes='" + notes + '\'' +
                ", metadata=" + metadata +
                ", attachedToDevices=" + attachedToDevices +
                ", groupsV2=" + groupsV2 +
                ", usersV2=" + usersV2 +
                ", type=" + type +
                ", mappedUnit='" + mappedUnit + '\'' +
                ", lastActiveNanos=" + lastActiveNanos +
                ", tenantId=" + tenantId +
                '}';
    }
}
