package com.sequenceiq.cloudbreak.cloud.aws.task;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollBooleanStateTask;

@Component(ASGroupStatusCheckerTask.NAME)
@Scope(value = "prototype")
public class ASGroupStatusCheckerTask extends PollBooleanStateTask {
    public static final String NAME = "aSGroupStatusCheckerTask";

    private static final int MAX_INSTANCE_ID_SIZE = 100;
    private static final int INSTANCE_RUNNING = 16;
    private static final Logger LOGGER = LoggerFactory.getLogger(ASGroupStatusCheckerTask.class);

    private String autoScalingGroupName;
    private Integer requiredInstances;
    private AwsClient awsClient;
    private CloudFormationStackUtil cloudFormationStackUtil;

    public ASGroupStatusCheckerTask(AuthenticatedContext authenticatedContext, String asGroupName, Integer requiredInstances, AwsClient awsClient,
            CloudFormationStackUtil cloudFormationStackUtil) {
        super(authenticatedContext, true);
        this.autoScalingGroupName = asGroupName;
        this.requiredInstances = requiredInstances;
        this.awsClient = awsClient;
        this.cloudFormationStackUtil = cloudFormationStackUtil;
    }

    @Override
    public Boolean call() {
        LOGGER.info("Checking status of Auto Scaling group '{}'", autoScalingGroupName);
        AmazonEC2Client amazonEC2Client = awsClient.createAccess(new AwsCredentialView(getAuthenticatedContext().getCloudCredential()),
                getAuthenticatedContext().getCloudContext().getLocation().getRegion().value());
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(new AwsCredentialView(getAuthenticatedContext().getCloudCredential()),
                getAuthenticatedContext().getCloudContext().getLocation().getRegion().value());
        List<String> instanceIds = cloudFormationStackUtil.getInstanceIds(amazonASClient, autoScalingGroupName);
        if (instanceIds.size() < requiredInstances) {
            LOGGER.debug("Instances in AS group: {}, needed: {}", instanceIds.size(), requiredInstances);
            return false;
        }
        List<DescribeInstanceStatusResult> describeInstanceStatusResultList = new ArrayList<>();

        List<List<String>> partitionedInstanceIdsList = Lists.partition(instanceIds, MAX_INSTANCE_ID_SIZE);

        for (List<String> partitionedInstanceIds : partitionedInstanceIdsList) {
            DescribeInstanceStatusRequest describeInstanceStatusRequest = new DescribeInstanceStatusRequest().withInstanceIds(partitionedInstanceIds);
            DescribeInstanceStatusResult describeResult = amazonEC2Client.describeInstanceStatus(describeInstanceStatusRequest);
            describeInstanceStatusResultList.add(describeResult);
        }

        List<InstanceStatus> instanceStatusList = describeInstanceStatusResultList.stream()
                .flatMap(describeInstanceStatusResult -> describeInstanceStatusResult.getInstanceStatuses().stream())
                .collect(Collectors.toList());

        if (instanceStatusList.size() < requiredInstances) {
            LOGGER.debug("Instances up: {}, needed: {}", instanceStatusList.size(), requiredInstances);
            return false;
        }

        for (InstanceStatus status : instanceStatusList) {
            if (INSTANCE_RUNNING != status.getInstanceState().getCode()) {
                LOGGER.debug("Instances are up but not all of them are in running state.");
                return false;
            }
        }
        return true;
    }

}
