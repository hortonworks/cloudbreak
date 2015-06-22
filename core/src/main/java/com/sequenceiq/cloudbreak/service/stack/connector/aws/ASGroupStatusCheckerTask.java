package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;


@Component
public class ASGroupStatusCheckerTask extends StackBasedStatusCheckerTask<AutoScalingGroupReadyContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ASGroupStatusCheckerTask.class);

    private static final int INSTANCE_RUNNING = 16;

    @Inject
    private CloudFormationStackUtil cfStackUtil;

    @Inject
    private AwsStackUtil awsStackUtil;

    @Override
    public boolean checkStatus(AutoScalingGroupReadyContext context) {
        LOGGER.info("Checking status of Auto Scaling group '{}'", context.getAutoScalingGroupName());
        AmazonEC2Client amazonEC2Client = awsStackUtil.createEC2Client(context.getStack());
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(context.getStack());
        List<String> instanceIds = cfStackUtil.getInstanceIds(context.getAutoScalingGroupName(), amazonASClient);
        if (instanceIds.size() < context.getRequiredInstances()) {
            LOGGER.debug("Instances in AS group: {}, needed: {}", instanceIds.size(), context.getRequiredInstances());
            return false;
        }
        DescribeInstanceStatusResult describeResult = amazonEC2Client.describeInstanceStatus(new DescribeInstanceStatusRequest().withInstanceIds(instanceIds));
        if (describeResult.getInstanceStatuses().size() < context.getRequiredInstances()) {
            LOGGER.debug("Instances up: {}, needed: {}", describeResult.getInstanceStatuses().size(), context.getRequiredInstances());
            return false;
        }
        for (InstanceStatus status : describeResult.getInstanceStatuses()) {
            if (INSTANCE_RUNNING != status.getInstanceState().getCode()) {
                LOGGER.debug("Instances are up but not all of them are in running state.");
                return false;
            }
        }
        return true;
    }

    @Override
    public void handleTimeout(AutoScalingGroupReadyContext t) {
        throw new AwsResourceException(String.format("Timeout while polling instances in Auto Scaling group '%s'.", t.getAutoScalingGroupName()));
    }

    @Override
    public String successMessage(AutoScalingGroupReadyContext t) {
        return String.format("Auto Scaling group '%s' is ready. All %s instances are running.", t.getAutoScalingGroupName(), t.getRequiredInstances());
    }

}
