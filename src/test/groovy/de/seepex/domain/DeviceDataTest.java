package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Do not touch and modify this class careless. This class defined structure gets send by RPC via CloudUploader from SPM
 * to bucket Service. If the structure changes hard, the receive at bucket Service can crash.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceDataTest  {

    private UUID deviceId;

    private Double value;

    private UUID sensorId;

    private UUID formulaId;

    private String username;

    private UUID tenantId;

    private Long timeInNanoSeconds;

    private Long receivedTimeInNanoSeconds;

    public Long getReceivedTimeInNanoSeconds() {
        return receivedTimeInNanoSeconds;
    }

    public void setReceivedTimeInNanoSeconds(Long receivedTimeInNanoSeconds) {
        this.receivedTimeInNanoSeconds = receivedTimeInNanoSeconds;
    }

    public Long getTimeInNanoSeconds() {
        return timeInNanoSeconds;
    }

    public void setTimeInNanoSeconds(Long timeInNanoSeconds) {
        this.timeInNanoSeconds = timeInNanoSeconds;
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public UUID getFormulaId() {
        return formulaId;
    }

    public void setFormulaId(UUID formulaId) {
        this.formulaId = formulaId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public void setSensorId(UUID sensorId) {
        this.sensorId = sensorId;
    }

    public UUID getSensorId() {
        return sensorId;
    }

    public Date getTimeAsDate() {
        return Date.from(Instant.EPOCH.plusNanos(timeInNanoSeconds));
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public String getSensorIdAsString() {
        return sensorId != null ? sensorId.toString() : null;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return "DeviceData{" +
                "deviceId=" + deviceId +
                ", value=" + value +
                ", sensorId=" + sensorId +
                ", formulaId=" + formulaId +
                ", username='" + username + '\'' +
                ", tenantId=" + tenantId +
                ", timeInNanoSeconds=" + timeInNanoSeconds +
                ", receivedTimeInNanoSeconds=" + receivedTimeInNanoSeconds +
                '}';
    }
}
