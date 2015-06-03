package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;

@Service
public class CloudFormationStackUtil {

    @Inject
    private AwsStackUtil awsStackUtil;

    public String getAutoscalingGroupName(Stack stack, String instanceGroup) {
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonCloudFormationClient amazonCfClient = awsStackUtil.createCloudFormationClient(Regions.valueOf(stack.getRegion()), awsCredential);
        return getAutoscalingGroupName(stack, amazonCfClient, instanceGroup);
    }

    public String getAutoscalingGroupName(Stack stack, AmazonCloudFormationClient amazonCFClient, String instanceGroup) {
        DescribeStackResourceResult asGroupResource = amazonCFClient.describeStackResource(new DescribeStackResourceRequest()
                .withStackName(stack.getResourcesByType(ResourceType.CLOUDFORMATION_STACK).get(0).getResourceName())
                .withLogicalResourceId(String.format("AmbariNodes%s", instanceGroup.replaceAll("_", ""))));
        return asGroupResource.getStackResourceDetail().getPhysicalResourceId();
    }

    public String getCfStackName(Stack stack) {
        return String.format("%s-%s", stack.getName(), stack.getId());
    }

    public List<String> getInstanceIds(Stack stack, String instanceGroup) {
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(Regions.valueOf(stack.getRegion()), awsCredential);
        AmazonCloudFormationClient amazonCFClient = awsStackUtil.createCloudFormationClient(Regions.valueOf(stack.getRegion()), awsCredential);
        return getInstanceIds(stack, amazonASClient, amazonCFClient, instanceGroup);
    }

    public List<String> getInstanceIds(Stack stack, AmazonAutoScalingClient amazonASClient, AmazonCloudFormationClient amazonCFClient, String instanceGroup) {
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonASClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                        .withAutoScalingGroupNames(getAutoscalingGroupName(stack, amazonCFClient, instanceGroup)));
        List<String> instanceIds = new ArrayList<>();
        if (describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances() != null) {
            for (Instance instance : describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances()) {
                instanceIds.add(instance.getInstanceId());
            }
        }
        return instanceIds;
    }

    public List<String> getInstanceIds(String asGroupName, AmazonAutoScalingClient amazonASClient) {
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonASClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(asGroupName));
        List<String> instanceIds = new ArrayList<>();
        if (describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances() != null) {
            for (Instance instance : describeAutoScalingGroupsResult.getAutoScalingGroups().get(0).getInstances()) {
                instanceIds.add(instance.getInstanceId());
            }
        }
        return instanceIds;
    }
}