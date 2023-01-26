package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeSensor {

    private UUID id;
    private String fieldId;
    private UUID time;
    private String name;
    private String location;
    private String unit;
    private String notes;
    private Map<String, String> metadata;
    private UUID type;
    private String mappedUnit;
    private List<FeDeviceSimple> devices = new ArrayList<>();
    private Long lastActiveNanos;
    private Boolean deprecated;

    public Boolean isDeprecated() {
        return deprecated != null ? deprecated : Boolean.FALSE;
    }

    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    public String getMappedUnit() {
        return mappedUnit;
    }

    public void setMappedUnit(String mappedUnit) {
        this.mappedUnit = mappedUnit;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFieldId() {
        return fieldId;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public UUID getTime() {
        return time;
    }

    public void setTime(UUID time) {
        this.time = time;
    }

    public String getName() {
        return name != null ? name : StringUtils.EMPTY;
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

    public UUID getType() {
        return type;
    }

    public void setType(UUID type) {
        this.type = type;
    }

    public List<FeDeviceSimple> getDevices() {
        return devices;
    }

    public void addDevice(FeDeviceSimple device) {
        this.devices.add(device);
    }

    public String getMappedOrRawUnit() {
        return mappedUnit != null ? mappedUnit : unit;
    }

    public void setDevices(List<FeDeviceSimple> devices) {
        this.devices = devices;
    }

    public Long getLastActiveNanos() {
        return lastActiveNanos;
    }

    public void setLastActiveNanos(Long lastActiveNanos) {
        this.lastActiveNanos = lastActiveNanos;
    }
}
