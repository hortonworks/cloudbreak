package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.ResourceType;
import com.sequenceiq.cloudbreak.domain.Stack;

@Service
public class CloudFormationStackUtil {

    @Autowired
    private AwsStackUtil awsStackUtil;

    public String getAutoscalingGroupName(Stack stack) {
        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonCloudFormationClient amazonCfClient = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
        return getAutoscalingGroupName(stack, amazonCfClient);
    }

    public String getAutoscalingGroupName(Stack stack, AmazonCloudFormationClient amazonCFClient) {
        DescribeStackResourceResult asGroupResource = amazonCFClient.describeStackResource(new DescribeStackResourceRequest()
                .withStackName(stack.getResourcesByType(ResourceType.CLOUDFORMATION_STACK).get(0).getResourceName())
                .withLogicalResourceId("AmbariNodes"));
        return asGroupResource.getStackResourceDetail().getPhysicalResourceId();
    }

    public List<String> getInstanceIds(Stack stack) {
        AwsTemplate awsTemplate = (AwsTemplate) stack.getTemplate();
        AwsCredential awsCredential = (AwsCredential) stack.getCredential();
        AmazonAutoScalingClient amazonASClient = awsStackUtil.createAutoScalingClient(awsTemplate.getRegion(), awsCredential);
        AmazonCloudFormationClient amazonCFClient = awsStackUtil.createCloudFormationClient(awsTemplate.getRegion(), awsCredential);
        return getInstanceIds(stack, amazonASClient, amazonCFClient);
    }

    public List<String> getInstanceIds(Stack stack, AmazonAutoScalingClient amazonASClient, AmazonCloudFormationClient amazonCFClient) {
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonASClient
                .describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(getAutoscalingGroupName(stack, amazonCFClient)));
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