package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;

@Service
public class CloudFormationStackUtil {

    @Inject
    private AwsClient awsClient;

    public String getAutoscalingGroupName(AuthenticatedContext ac, String instanceGroup, CloudResource cloudResource) {
        AmazonCloudFormationClient amazonCfClient = awsClient.createCloudFormationClient(ac.getCloudCredential());
        return getAutoscalingGroupName(ac, amazonCfClient, instanceGroup);
    }

    public String getAutoscalingGroupName(AuthenticatedContext ac, AmazonCloudFormationClient amazonCFClient, String instanceGroup) {
        DescribeStackResourceResult asGroupResource = amazonCFClient.describeStackResource(new DescribeStackResourceRequest()
                .withStackName(ac.getCloudContext().getName())
                .withLogicalResourceId(String.format("AmbariNodes%s", instanceGroup.replaceAll("_", ""))));
        return asGroupResource.getStackResourceDetail().getPhysicalResourceId();
    }

    public String getCfStackName(AuthenticatedContext ac) {
        return String.format("%s-%s", ac.getCloudContext().getName(), ac.getCloudContext().getId());
    }

    public List<String> getInstanceIds(AuthenticatedContext ac, CloudStack stack, String instanceGroup) {
        AmazonAutoScalingClient amazonASClient = awsClient.createAutoScalingClient(ac.getCloudCredential());
        AmazonCloudFormationClient amazonCFClient = awsClient.createCloudFormationClient(ac.getCloudCredential());
        return getInstanceIds(ac, amazonASClient, amazonCFClient, instanceGroup);
    }

    public List<String> getInstanceIds(AuthenticatedContext ac, AmazonAutoScalingClient aSClient, AmazonCloudFormationClient amazonCFClient, String ig) {
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = aSClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
                        .withAutoScalingGroupNames(getAutoscalingGroupName(ac, amazonCFClient, ig)));
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