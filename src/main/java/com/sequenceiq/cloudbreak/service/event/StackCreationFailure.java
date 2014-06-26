package com.sequenceiq.cloudbreak.service.event;

import com.sequenceiq.cloudbreak.domain.Stack;

public class StackCreationFailure {

    private Stack stack;
    private String detailedMessage;

    public StackCreationFailure(Stack stack, String detailedMessage) {
        this.stack = stack;
        this.detailedMessage = detailedMessage;
    }

    public Stack getStack() {
        return stack;
    }

    public void setStack(Stack stack) {
        this.stack = stack;
    }

    public String getDetailedMessage() {
        return detailedMessage;
    }

    public void setDetailedMessage(String detailedMessage) {
        this.detailedMessage = detailedMessage;
    }

}
