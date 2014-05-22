package com.sequenceiq.provisioning.domain;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;

public class AwsCloudInstanceDescription extends CloudInstanceDescription {

    private DescribeStacksResult stack;
    private DescribeStackResourcesResult resources;

    public AwsCloudInstanceDescription(DescribeStacksResult stack, DescribeStackResourcesResult resources) {
        this.setStack(stack);
        this.resources = resources;
    }

    public DescribeStacksResult getStack() {
        return stack;
    }

    public void setStack(DescribeStacksResult stack) {
        this.stack = stack;
    }

    public DescribeStackResourcesResult getResources() {
        return resources;
    }

    public void setResources(DescribeStackResourcesResult resources) {
        this.resources = resources;
    }

}
