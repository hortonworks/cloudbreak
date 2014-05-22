package com.sequenceiq.provisioning.domain;

import com.amazonaws.services.cloudformation.model.DescribeStacksResult;

public class AwsCloudInstanceDescription extends CloudInstanceDescription {

    private DescribeStacksResult stack;

    public AwsCloudInstanceDescription(DescribeStacksResult stack) {
        this.stack = stack;
    }

    public DescribeStacksResult getStack() {
        return stack;
    }

    public void setStack(DescribeStacksResult stack) {
        this.stack = stack;
    }
}
