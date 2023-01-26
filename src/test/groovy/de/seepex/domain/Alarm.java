package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.List;
import java.util.UUID;

public class Alarm {

    private static final long serialVersionUID = 102L;

    private UUID id;

    private String name;

    private String description;

    @Deprecated
    @JsonAlias({"deviceCommNr", "device_comm_nr"})
    private String deviceCommNr;

    @JsonAlias({"deviceId", "device_id"})
    private UUID deviceId;

    private String level;

    @JsonAlias({"nanoSinceEpoch", "nano_since_epoch"})
    private Long nanoSinceEpoch;

    @JsonAlias({"sourceId", "source_id"})
    private UUID sourceId;

    @JsonAlias({"sourceDescription", "source_description"})
    private String sourceDescription;

    @JsonAlias({"thresholdMin", "threshold_min"})
    private Double thresholdMin;

    @JsonAlias({"thresholdMax", "threshold_max"})
    private Double thresholdMax;

    private Double value;

    private Boolean heartbeat;

    @JsonAlias({"groupsv2", "groups_v2"})
    private List<UUID> groupsv2;

    @JsonAlias({"usersV2", "users_v2"})
    private List<UUID> usersV2;

    private Boolean acknowledged;

    @JsonAlias({"alarmConfigurationId", "alarm_configuration_id"})
    private UUID alarmConfigurationId;

    private String comment;

    @JsonAlias({"tenantId", "tenant_id"})
    private UUID tenantId;

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

    public String getDeviceCommNr() {
        return deviceCommNr;
    }

    public void setDeviceCommNr(String deviceCommNr) {
        this.deviceCommNr = deviceCommNr;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Long getNanoSinceEpoch() {
        return nanoSinceEpoch;
    }

    public void setNanoSinceEpoch(Long nanoSinceEpoch) {
        this.nanoSinceEpoch = nanoSinceEpoch;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }

    public void setSourceDescription(String sourceDescription) {
        this.sourceDescription = sourceDescription;
    }

    public Double getThresholdMin() {
        return thresholdMin;
    }

    public void setThresholdMin(Double thresholdMin) {
        this.thresholdMin = thresholdMin;
    }

    public Double getThresholdMax() {
        return thresholdMax;
    }

    public void setThresholdMax(Double thresholdMax) {
        this.thresholdMax = thresholdMax;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Boolean getHeartbeat() {
        return heartbeat;
    }

    public void setHeartbeat(Boolean heartbeat) {
        this.heartbeat = heartbeat;
    }

    public List<UUID> getGroupsv2() {
        return groupsv2;
    }

    public void setGroupsv2(List<UUID> groupsv2) {
        this.groupsv2 = groupsv2;
    }

    public List<UUID> getUsersV2() {
        return usersV2;
    }

    public void setUsersV2(List<UUID> usersV2) {
        this.usersV2 = usersV2;
    }

    public Boolean getAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(Boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public UUID getAlarmConfigurationId() {
        return alarmConfigurationId;
    }

    public void setAlarmConfigurationId(UUID alarmConfigurationId) {
        this.alarmConfigurationId = alarmConfigurationId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
}
