package com.sequenceiq.provisioning.domain;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;

public class DetailedAwsStackDescription extends StackDescription {

    private DescribeStacksResult stack;
    private DescribeStackResourcesResult resources;
    private DescribeInstancesResult instances;

    public DetailedAwsStackDescription(DescribeStacksResult stack, DescribeStackResourcesResult resources, DescribeInstancesResult instances) {
        this.stack = stack;
        this.resources = resources;
        this.instances = instances;
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

    public DescribeInstancesResult getInstances() {
        return instances;
    }

    public void setInstances(DescribeInstancesResult instances) {
        this.instances = instances;
    }

}
