package com.sequenceiq.cloudbreak.service.event;


public class StackCreationFailure {

    private Long stackId;
    private String detailedMessage;

    public StackCreationFailure(Long stackId, String detailedMessage) {
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
