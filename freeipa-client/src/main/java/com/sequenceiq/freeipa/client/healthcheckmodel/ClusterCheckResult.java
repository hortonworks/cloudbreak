package com.sequenceiq.freeipa.client.healthcheckmodel;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClusterCheckResult {

    private String status;

    private String host;

    private List<CheckResult> replicas;

    @JsonProperty("plugin_stat")
    private List<PluginStatusEntry> pluginStat;

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

    public List<CheckResult> getReplicas() {
        return replicas;
    }

    public void setReplicas(List<CheckResult> replicas) {
        this.replicas = replicas;
    }

    public List<PluginStatusEntry> getPluginStats() {
        return pluginStat;
    }

    public void setPluginStats(List<PluginStatusEntry> pluginStat) {
        this.pluginStat = pluginStat;
    }

    @Override
    public String toString() {
        return "ClusterCheckResult{"
                + "status='" + status + "\',"
                + "host='" + host + "\',"
                + "replicas={" + StringUtils.join(replicas, ",") + "},"
                + "plugin_stat={" + StringUtils.join(pluginStat, ",") + "}"
                + '}';
    }
}