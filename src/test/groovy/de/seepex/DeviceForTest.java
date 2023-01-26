package de.seepex;

import java.util.HashMap;
import java.util.UUID;

public class DeviceForTest {

    private UUID id;
    private HashMap<String, String> config;
    private String serialNumber;

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public HashMap<String, String> getConfig() {
        return config;
    }

    public void setConfig(HashMap<String, String> config) {
        this.config = config;
    }
}
