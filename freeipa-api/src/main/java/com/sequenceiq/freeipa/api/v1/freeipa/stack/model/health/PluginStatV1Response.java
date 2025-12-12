package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PluginStatV1Response {

    private String host;

    private String plugin;

    private int responseTime;

    private String status;

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

    public int getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(int responseTime) {
        this.responseTime = responseTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "PluginStatV1Response{" +
                "host='" + host + '\'' +
                ", plugin='" + plugin + '\'' +
                ", responseTime=" + responseTime +
                ", status='" + status + '\'' +
                '}';
    }
}
