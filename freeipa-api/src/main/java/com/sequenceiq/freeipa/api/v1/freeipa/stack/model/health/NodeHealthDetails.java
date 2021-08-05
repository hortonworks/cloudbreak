package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;

import io.swagger.annotations.ApiModel;

@ApiModel("NodeHealthDetails")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeHealthDetails {

    @NotNull
    private List<String> issues;

    @NotNull
    private InstanceStatus status;

    @NotNull
    private String name;

    @NotNull
    private String instanceId;

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

    @Override
    public String toString() {
        return "NodeHealthDetails{" +
                "issues=" + issues +
                ", status=" + status +
                ", name='" + name + '\'' +
                ", instanceId='" + instanceId + '\'' +
                '}';
    }
}
