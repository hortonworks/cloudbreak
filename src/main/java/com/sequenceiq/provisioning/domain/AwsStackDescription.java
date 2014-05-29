package com.sequenceiq.provisioning.domain;

import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;

public class AwsStackDescription extends StackDescription {

    private DescribeStacksResult stack;
    private DescribeInstancesResult instances;

    public AwsStackDescription(DescribeStacksResult stack, DescribeInstancesResult instances) {
        this.stack = stack;
        this.instances = instances;
    }

    public DescribeStacksResult getStack() {
        return stack;
    }

    public void setStack(DescribeStacksResult stack) {
        this.stack = stack;
    }

    public DescribeInstancesResult getInstances() {
        return instances;
    }

    public void setInstances(DescribeInstancesResult instances) {
        this.instances = instances;
    }
}
