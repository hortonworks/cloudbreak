package com.sequenceiq.provisioning.domain;

import com.amazonaws.services.cloudformation.model.DescribeStacksResult;

public class AwsStackDescription extends StackDescription {

    private DescribeStacksResult stack;

    public AwsStackDescription(DescribeStacksResult stack) {
        this.stack = stack;
    }

    public DescribeStacksResult getStack() {
        return stack;
    }

    public void setStack(DescribeStacksResult stack) {
        this.stack = stack;
    }
}
