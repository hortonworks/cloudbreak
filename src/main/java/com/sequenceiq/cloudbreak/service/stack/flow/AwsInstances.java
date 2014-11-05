package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.List;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.sequenceiq.cloudbreak.domain.Stack;

public class AwsInstances extends AbstractInstances {

    private AmazonEC2Client amazonEC2Client;

    public AwsInstances(Stack stack, AmazonEC2Client amazonEC2Client, List<String> instances, String status) {
        super(stack, instances, status);
        this.amazonEC2Client = amazonEC2Client;
    }

    public AmazonEC2Client getAmazonEC2Client() {
        return amazonEC2Client;
    }

    public void setAmazonEC2Client(AmazonEC2Client amazonEC2Client) {
        this.amazonEC2Client = amazonEC2Client;
    }
}
