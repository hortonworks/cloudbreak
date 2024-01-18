package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class StackUpscaleFailedConclusionRequest extends StackEvent {

    private Map<String, Set<String>> hostgroupWithHostnames;

    private Map<String, Integer> hostGroupWithAdjustment;

    private boolean repair;

    private Exception exception;

    @JsonCreator
    public StackUpscaleFailedConclusionRequest(@JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostgroupWithHostnames") Map<String, Set<String>> hostgroupWithHostnames,
            @JsonProperty("hostGroupWithAdjustment") Map<String, Integer> hostGroupWithAdjustment,
            @JsonProperty("repair") boolean repair,
            @JsonProperty("exception") Exception exception) {
        super(stackId);
        this.repair = repair;
    }

    public Map<String, Set<String>> getHostgroupWithHostnames() {
        return hostgroupWithHostnames;
    }

    public void setHostgroupWithHostnames(Map<String, Set<String>> hostgroupWithHostnames) {
        this.hostgroupWithHostnames = hostgroupWithHostnames;
    }

    public Map<String, Integer> getHostGroupWithAdjustment() {
        return hostGroupWithAdjustment;
    }

    public void setHostGroupWithAdjustment(Map<String, Integer> hostGroupWithAdjustment) {
        this.hostGroupWithAdjustment = hostGroupWithAdjustment;
    }

    public boolean isRepair() {
        return repair;
    }

    public void setRepair(boolean repair) {
        this.repair = repair;
    }

    @Override
    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    @Override
    public String toString() {
        return "StackUpscaleFailedConclusionRequest{" +
                "hostgroupWithHostnames=" + hostgroupWithHostnames +
                ", hostGroupWithAdjustment=" + hostGroupWithAdjustment +
                ", repair=" + repair +
                ", exception=" + exception +
                "} " + super.toString();
    }
}
