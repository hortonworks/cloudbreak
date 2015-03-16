package com.sequenceiq.cloudbreak.core.flow.context;

public class StackStatusUpdateContext implements FlowContext {
    private Long stackId;
    private boolean start;
    private boolean pollingError;
    private String statusReason;

    public StackStatusUpdateContext(Long stackId, boolean start) {
        this.stackId = stackId;
        this.start = start;
    }

    public StackStatusUpdateContext(Long stackId, boolean start, String statusReason) {
        this.stackId = stackId;
        this.start = start;
        this.statusReason = statusReason;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }

    public boolean isPollingError() {
        return pollingError;
    }

    public void setPollingError(boolean pollingError) {
        this.pollingError = pollingError;
    }
}
