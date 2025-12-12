package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthCheckV1Response {

    private List<CheckResultV1Response> checks;

    private String host;

    private List<PluginStatV1Response> pluginStat;

    private String status;

    public List<CheckResultV1Response> getChecks() {
        return checks;
    }

    public void setChecks(List<CheckResultV1Response> checks) {
        this.checks = checks;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<PluginStatV1Response> getPluginStat() {
        return pluginStat;
    }

    public void setPluginStat(List<PluginStatV1Response> pluginStat) {
        this.pluginStat = pluginStat;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "HealthCheckV1Response{" +
                "checks=" + checks +
                ", host='" + host + '\'' +
                ", pluginStat=" + pluginStat +
                ", status='" + status + '\'' +
                '}';
    }
}
