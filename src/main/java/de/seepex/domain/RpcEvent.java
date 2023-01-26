package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RpcEvent {

    private RpcEventType type;
    private String hostname;
    private Long timestamp;

    @JsonAlias({"correlation_id"})
    private String correlationId;

    public enum RpcEventType {
        RECEIVE_CALL
    }

    public RpcEventType getType() {
        return type;
    }

    public void setType(RpcEventType type) {
        this.type = type;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        return "RpcEvent{" +
                "type=" + type +
                ", hostname='" + hostname + '\'' +
                ", timestamp=" + timestamp +
                ", correlationId='" + correlationId + '\'' +
                '}';
    }
}
