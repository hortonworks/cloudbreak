package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "NodeHealthDetails")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeHealthDetails {

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<String> issues = new ArrayList<>();

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private InstanceStatus status;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String instanceId;

    @NotNull
    private List<HealthCheckV1Response> healthChecks = new ArrayList<>();

    public InstanceStatus getStatus() {
        return status;
    }

    public void setStatus(InstanceStatus status) {
        this.status = status;
    }

    public List<String> getIssues() {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    public void addIssue(String issue) {
        if (issues == null) {
            issues = new ArrayList<>();
        }
        issues.add(issue);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public List<HealthCheckV1Response> getHealthChecks() {
        return healthChecks;
    }

    public void setHealthChecks(List<HealthCheckV1Response> healthChecks) {
        this.healthChecks = healthChecks;
    }

    @Override
    public String toString() {
        return "NodeHealthDetails{" +
                "issues=" + issues +
                ", status=" + status +
                ", name='" + name + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", healthChecks='" + healthChecks + '\'' +
                '}';
    }
}
