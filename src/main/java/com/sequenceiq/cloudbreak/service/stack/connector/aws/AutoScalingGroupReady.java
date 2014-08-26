package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.ec2.AmazonEC2Client;

public class AutoScalingGroupReady {

    private AmazonEC2Client amazonEC2Client;
    private AmazonAutoScalingClient amazonASClient;

    private String autoScalingGroupName;
    private Integer requiredInstances;

    public AutoScalingGroupReady(AmazonEC2Client amazonEC2Client, AmazonAutoScalingClient amazonASClient, String asGroupName, Integer requiredInstances) {
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
