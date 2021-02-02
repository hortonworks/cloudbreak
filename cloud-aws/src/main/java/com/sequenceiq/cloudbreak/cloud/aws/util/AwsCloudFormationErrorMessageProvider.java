package com.sequenceiq.cloudbreak.cloud.aws.util;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sequenceiq.cloudbreak.cloud.aws.AwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Component
public class AwsCloudFormationErrorMessageProvider {

    @Inject
    private AwsClient awsClient;

    @Inject
    private AwsEncodedAuthorizationFailureMessageDecoder awsEncodedAuthorizationFailureMessageDecoder;

    public String getErrorReason(AuthenticatedContext ac, String stackName, ResourceStatus resourceErrorStatus) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();

        return getErrorReason(credentialView, regionName, stackName, resourceErrorStatus);
    }

    public String getErrorReason(AwsCredentialView credentialView, String region, String stackName, ResourceStatus resourceErrorStatus) {
        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, region);
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackName);
        DescribeStacksResult describeStacksResult = cfClient.describeStacks(describeStacksRequest);
        String stackStatusReason = describeStacksResult.getStacks().get(0).getStackStatusReason();

        DescribeStackResourcesRequest describeStackResourcesRequest = new DescribeStackResourcesRequest().withStackName(stackName);
        DescribeStackResourcesResult describeStackResourcesResult = cfClient.describeStackResources(describeStackResourcesRequest);
        String stackResourceStatusReasons = describeStackResourcesResult.getStackResources().stream()
                .filter(stackResource -> ResourceStatus.fromValue(stackResource.getResourceStatus()).equals(resourceErrorStatus))
                .map(stackResource -> getStackResourceMessage(credentialView, region, stackResource))
                .collect(Collectors.joining(", "));

        return stackStatusReason + " " + stackResourceStatusReasons;
    }

    private String getStackResourceMessage(AwsCredentialView credentialView, String region, StackResource stackResource) {
        String statusReason = awsEncodedAuthorizationFailureMessageDecoder
                .decodeAuthorizationFailureMessageIfNeeded(credentialView, region, stackResource.getResourceStatusReason());
        return stackResource.getLogicalResourceId() + ": " + statusReason;
    }
}
