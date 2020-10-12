package com.sequenceiq.freeipa.client.healthcheckmodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckEntry {

    private String status;

    @JsonProperty("check_id")
    private String checkId;

    private String plugin;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCheckId() {
        return checkId;
    }

    public void setCheckId(String checkId) {
        this.checkId = checkId;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    @Override
    public String toString() {
        return "Health{"
                + "status='" + status + "\',"
                + "check_id='" + checkId + "\',"
                + "plugin='" + plugin + "\'"
                + '}';
    }
}