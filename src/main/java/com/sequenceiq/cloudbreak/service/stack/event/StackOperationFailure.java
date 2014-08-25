package com.sequenceiq.cloudbreak.service.stack.event;


public class StackOperationFailure {

    private Long stackId;
    private String detailedMessage;

    public StackOperationFailure(Long stackId, String detailedMessage) {
        this.stackId = stackId;
        this.detailedMessage = detailedMessage;
    }

    public Long getStackId() {
        return stackId;
    }

    public void setStackId(Long stackId) {
        this.stackId = stackId;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

}
