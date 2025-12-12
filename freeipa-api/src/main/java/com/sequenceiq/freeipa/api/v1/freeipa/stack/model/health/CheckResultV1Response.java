package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckResultV1Response {

    private String checkId;

    private String plugin;

    private String status;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckResultV1Response that = (CheckResultV1Response) o;
        return Objects.equals(checkId, that.checkId) && Objects.equals(plugin, that.plugin) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checkId, plugin, status);
    }

    @Override
    public String toString() {
        return "CheckResultV1Response{" +
                "checkId='" + checkId + '\'' +
                ", plugin='" + plugin + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
