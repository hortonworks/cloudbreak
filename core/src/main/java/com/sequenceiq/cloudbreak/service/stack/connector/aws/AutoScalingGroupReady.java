package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AutoScalingGroupReady extends StackContext {

    private AmazonEC2Client amazonEC2Client;
    private AmazonAutoScalingClient amazonASClient;
    private String autoScalingGroupName;
    private Integer requiredInstances;

    public AutoScalingGroupReady(Stack stack, AmazonEC2Client amazonEC2Client, AmazonAutoScalingClient amazonASClient,
            String asGroupName, Integer requiredInstances) {
        super(stack);
        this.amazonEC2Client = amazonEC2Client;
        this.amazonASClient = amazonASClient;
        this.autoScalingGroupName = asGroupName;
        this.requiredInstances = requiredInstances;
    }

    public AmazonEC2Client getAmazonEC2Client() {
        return amazonEC2Client;
    }

    public AmazonAutoScalingClient getAmazonASClient() {
        return amazonASClient;
    }

    public String getAutoScalingGroupName() {
        return autoScalingGroupName;
    }

    public Integer getRequiredInstances() {
        return requiredInstances;
    }

}
