package com.sequenceiq.cloudbreak.cloud.aws.util;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudFormationErrorMessageProvider.class);

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
        LOGGER.debug("Getting error reason in Cloudformation stack {} for resources with {} status", stackName, resourceErrorStatus);

        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, region);
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackName);
        DescribeStacksResult describeStacksResult = cfClient.describeStacks(describeStacksRequest);
        LOGGER.debug("Cloudformation stack {} describe result: {}", stackName, describeStacksResult);
        String stackStatusReason = describeStacksResult.getStacks().get(0).getStackStatusReason();
        LOGGER.debug("Cloudformation stack {} has the error status reason: {}", stackName, stackStatusReason);

        DescribeStackResourcesRequest describeStackResourcesRequest = new DescribeStackResourcesRequest().withStackName(stackName);
        DescribeStackResourcesResult describeStackResourcesResult = cfClient.describeStackResources(describeStackResourcesRequest);
        String stackResourceStatusReasons = describeStackResourcesResult.getStackResources().stream()
                .filter(stackResource -> ResourceStatus.fromValue(stackResource.getResourceStatus()).equals(resourceErrorStatus))
                .map(stackResource -> getStackResourceMessage(credentialView, region, stackResource))
                .collect(Collectors.joining(", "));

        String errorReason = stackStatusReason + " " + stackResourceStatusReasons;
        LOGGER.debug("Cloudformation stack {} has the following error reason: {}", stackName, errorReason);
        return errorReason;
    }

    private String getStackResourceMessage(AwsCredentialView credentialView, String region, StackResource stackResource) {
        String resourceId = stackResource.getLogicalResourceId();
        String statusReason = awsEncodedAuthorizationFailureMessageDecoder
                .decodeAuthorizationFailureMessageIfNeeded(credentialView, region, stackResource.getResourceStatusReason());
        LOGGER.debug("Cloudformation resource {} has the error status reason: {}", resourceId, statusReason);
        return resourceId + ": " + statusReason;
    }
}
