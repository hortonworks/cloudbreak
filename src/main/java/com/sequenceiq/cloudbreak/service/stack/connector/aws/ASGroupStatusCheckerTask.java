package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.AddInstancesFailedException;

@Component
public class ASGroupStatusCheckerTask implements StatusCheckerTask<AutoScalingGroupReady> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ASGroupStatusCheckerTask.class);

    private static final int INSTANCE_RUNNING = 16;

    @Autowired
    private CloudFormationStackUtil cfStackUtil;

    @Override
    public boolean checkStatus(AutoScalingGroupReady asGroupReady) {
        LOGGER.info("Checking status of autoscaling group '{}'", asGroupReady.getAutoScalingGroupName());
        AmazonEC2Client amazonEC2Client = asGroupReady.getAmazonEC2Client();
        List<String> instanceIds = cfStackUtil.getInstanceIds(asGroupReady.getAutoScalingGroupName(), asGroupReady.getAmazonASClient());
        if (instanceIds.size() < asGroupReady.getRequiredInstances()) {
            return false;
        }
        DescribeInstanceStatusResult describeResult = amazonEC2Client.describeInstanceStatus(new DescribeInstanceStatusRequest().withInstanceIds(instanceIds));
        if (describeResult.getInstanceStatuses().size() < asGroupReady.getRequiredInstances()) {
            return false;
        }
        for (InstanceStatus status : describeResult.getInstanceStatuses()) {
            if (INSTANCE_RUNNING != status.getInstanceState().getCode()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void handleTimeout(AutoScalingGroupReady t) {
        throw new AddInstancesFailedException(String.format(
                "Something went wrong. Instances in Auto Scaling group '%s' not started in a reasonable timeframe.",
                t.getAutoScalingGroupName()));
    }

    @Override
    public String successMessage(AutoScalingGroupReady t) {
        return String.format("AutoScaling group '%s' is ready. All %s instances are running.", t.getAutoScalingGroupName(), t.getRequiredInstances());
    }
}
