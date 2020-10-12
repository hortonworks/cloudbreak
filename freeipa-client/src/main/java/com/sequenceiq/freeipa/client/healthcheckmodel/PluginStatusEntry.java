package com.sequenceiq.freeipa.client.healthcheckmodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PluginStatusEntry {

    private String status;

    private String host;

    private String plugin;

    @JsonProperty("response_time")
    private Integer responseTime;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPlugin() {
        return plugin;
    }

    public void setPlugin(String plugin) {
        this.plugin = plugin;
    }

    public Integer getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Integer responseTime) {
        this.responseTime = responseTime;
    }

    @Override
    public String toString() {
        return "PluginStatusEntry{"
                + "status='" + status + "\',"
                + "host='" + host + "\',"
                + "plugin='" + plugin + "\',"
                + "response_time=" + responseTime
                + '}';
    }
}