package com.sequenceiq.freeipa.client.healthcheckmodel;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CheckResult {

    private String status;

    private String host;

    private List<CheckEntry> checks;

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

    public List<CheckEntry> getChecks() {
        return checks;
    }

    public void setChecks(List<CheckEntry> checks) {
        this.checks = checks;
    }

    public List<PluginStatusEntry> getPluginStats() {
        return pluginStat;
    }

    public void setPluginStats(List<PluginStatusEntry> pluginStat) {
        this.pluginStat = pluginStat;
    }

    @Override
    public String toString() {
        return "CheckResult{"
                + "status='" + status + "',"
                + "host='" + host + "',"
                + "checks={" + StringUtils.join(checks, ",") + "},"
                + "plugin_stat={" + StringUtils.join(pluginStat, ",") +  "}"
                + '}';
    }
}