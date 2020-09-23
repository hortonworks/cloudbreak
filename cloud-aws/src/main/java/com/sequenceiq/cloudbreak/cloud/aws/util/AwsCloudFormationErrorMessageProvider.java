package com.sequenceiq.cloudbreak.cloud.aws.util;

import java.util.stream.Collectors;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;

public class AwsCloudFormationErrorMessageProvider {

    private AwsCloudFormationErrorMessageProvider() {
    }

    public static String getErrorReason(AmazonCloudFormationRetryClient cfRetryClient, String stackName, ResourceStatus resourceErrorStatus) {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackName);
        DescribeStacksResult describeStacksResult = cfRetryClient.describeStacks(describeStacksRequest);
        String stackStatusReason = describeStacksResult.getStacks().get(0).getStackStatusReason();

        DescribeStackResourcesRequest describeStackResourcesRequest = new DescribeStackResourcesRequest().withStackName(stackName);
        DescribeStackResourcesResult describeStackResourcesResult = cfRetryClient.describeStackResources(describeStackResourcesRequest);
        String stackResourceStatusReasons = describeStackResourcesResult.getStackResources().stream()
                .filter(stackResource -> ResourceStatus.fromValue(stackResource.getResourceStatus()).equals(resourceErrorStatus))
                .map(stackResource -> stackResource.getLogicalResourceId() + ": " + stackResource.getResourceStatusReason())
                .collect(Collectors.joining(", "));

        return stackStatusReason + " " + stackResourceStatusReasons;
    }
}
