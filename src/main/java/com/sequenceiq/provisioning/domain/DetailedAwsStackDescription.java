package com.sequenceiq.provisioning.domain;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;

public class DetailedAwsStackDescription extends StackDescription {

    private DescribeStacksResult stack;
    private DescribeStackResourcesResult resources;

    public DetailedAwsStackDescription(DescribeStacksResult stack, DescribeStackResourcesResult resources) {
        this.stack = stack;
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
