package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.rollingvs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RollingVerticalScaleResult {
    private final Map<String, Status> instanceStatus;

    private final List<String> instanceIds;

    private final String group;

    public RollingVerticalScaleResult(List<String> instanceIds, String group) {
        this.instanceIds = instanceIds;
        this.group = group;
        this.instanceStatus = new HashMap<>();

        for (String instanceId : instanceIds) {
            this.instanceStatus.put(instanceId, new Status());
        }
    }

    @JsonCreator
    public RollingVerticalScaleResult(
            @JsonProperty("instanceIds") List<String> instanceIds,
            @JsonProperty("group") String group,
            @JsonProperty("instanceStatus") Map<String, Status> instanceStatus) {
        this.instanceIds = instanceIds != null ? instanceIds : new ArrayList<>();
        this.group = group;
        this.instanceStatus = instanceStatus != null ? instanceStatus : new HashMap<>();

        // Initialize status map if needed
        if (this.instanceStatus.isEmpty()) {
            for (String instanceId : this.instanceIds) {
                this.instanceStatus.put(instanceId, new Status());
            }
        }
    }

    public Status getStatus(String instanceId) {
        return instanceStatus.get(instanceId);
    }

    public void setStatus(String instanceId, RollingVerticalScaleStatus status) {
        instanceStatus.get(instanceId).setStatus(status);
    }

    public void setStatus(String instanceId, RollingVerticalScaleStatus status, String errorMessage) {
        instanceStatus.get(instanceId).setStatus(status, errorMessage);
    }

    public List<String> getInstanceIds() {
        return instanceIds;
    }

    public String getGroup() {
        return group;
    }

    public Map<String, Status> getInstanceStatus() {
        return instanceStatus;
    }

    public static class Status {
        private RollingVerticalScaleStatus status;

        private final List<String> messages;

        Status() {
            this.status = RollingVerticalScaleStatus.INIT;
            this.messages = new ArrayList<>();
        }

        public RollingVerticalScaleStatus getStatus() {
            return status;
        }

        public void setStatus(RollingVerticalScaleStatus status) {
            this.status = status;
        }

        public void setStatus(RollingVerticalScaleStatus status, String message) {
            this.status = status;
            this.messages.add(message);
        }

        public String getMessage() {
            return String.join(",", messages);
        }

        public void setMessage(String message) {
            this.messages.add(message);
        }
    }
}
