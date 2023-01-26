package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FeDeviceSimple {

    private UUID id;
    private String commNr;
    private String tag;
    private String type;
    private String notes;
    private String customer;
    private String description;
    private Map<String, String> configuration;
    private String picture;
    private String thumbnail;
    private Long lastActiveNanoSeconds;
    private Long createDateNanoSeconds;
    private UUID manufacturerId;

    public UUID getManufacturerId() {
        return manufacturerId;
    }

    public void setManufacturerId(UUID manufacturerId) {
        this.manufacturerId = manufacturerId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCommNr() {
        return commNr;
    }

    public void setCommNr(String commNr) {
        this.commNr = commNr;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
    }

    public String getPicture() {
        return picture;
    }

    public void setPicture(String picture) {
        this.picture = picture;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Long getLastActiveNanoSeconds() {
        return lastActiveNanoSeconds;
    }

    public void setLastActiveNanoSeconds(Long lastActiveNanoSeconds) {
        this.lastActiveNanoSeconds = lastActiveNanoSeconds;
    }

    public Long getCreateDateNanoSeconds() {
        return createDateNanoSeconds;
    }

    public void setCreateDateNanoSeconds(Long createDateNanoSeconds) {
        this.createDateNanoSeconds = createDateNanoSeconds;
    }
}
