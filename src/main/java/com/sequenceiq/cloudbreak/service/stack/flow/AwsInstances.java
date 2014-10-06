package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2Client;

public class AwsInstances extends AbstractInstances {

    private AmazonEC2Client amazonEC2Client;

    public AwsInstances(long stackId, AmazonEC2Client amazonEC2Client, List<String> instances, String status) {
        super(stackId, instances, status);
        this.amazonEC2Client = amazonEC2Client;
    }

    public AmazonEC2Client getAmazonEC2Client() {
        return amazonEC2Client;
    }

    public void setAmazonEC2Client(AmazonEC2Client amazonEC2Client) {
        this.amazonEC2Client = amazonEC2Client;
    }
}
