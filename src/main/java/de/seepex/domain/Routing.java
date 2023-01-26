package de.seepex.domain;

public class Routing {

    private String serviceId;
    private String routingKey;
    private Boolean exclusive;
    private Boolean delete = false;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public Boolean getExclusive() {
        return exclusive;
    }

    public void setExclusive(Boolean exclusive) {
        this.exclusive = exclusive;
    }

    public Boolean getDelete() {
        return delete;
    }

    public void setDelete(Boolean delete) {
        this.delete = delete;
    }

    @Override
    public String toString() {
        return "Routing{" +
                "serviceId='" + serviceId + '\'' +
                ", routingKey='" + routingKey + '\'' +
                ", exclusive=" + exclusive +
                ", delete=" + delete +
                '}';
    }
}
