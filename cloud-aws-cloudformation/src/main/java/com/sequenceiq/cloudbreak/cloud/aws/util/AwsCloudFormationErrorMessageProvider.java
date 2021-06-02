package com.sequenceiq.cloudbreak.cloud.aws.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.LegacyAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Component
public class AwsCloudFormationErrorMessageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudFormationErrorMessageProvider.class);

    @Inject
    private LegacyAwsClient awsClient;

    @Inject
    private AwsEncodedAuthorizationFailureMessageDecoder awsEncodedAuthorizationFailureMessageDecoder;

    public String getErrorReason(AuthenticatedContext ac, String stackName, ResourceStatus... resourceErrorStatuses) {
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();

        return getErrorReason(credentialView, regionName, stackName, resourceErrorStatuses);
    }

    public String getErrorReason(AwsCredentialView credentialView, String region, String stackName, ResourceStatus... resourceErrorStatuses) {
        LOGGER.debug("Getting error reason in Cloudformation stack {} for resources with {} status", stackName, resourceErrorStatuses);

        AmazonCloudFormationClient cfClient = awsClient.createCloudFormationClient(credentialView, region);
        String stackStatusReason = getStackStatusReason(stackName, cfClient);
        List<StackEvent> cfEvents = getCfEvents(stackName, cfClient);

        Set<String> resourceErrorStatusesSet = Arrays.stream(resourceErrorStatuses).map(ResourceStatus::name).collect(Collectors.toSet());

        String stackResourceStatusReasons = getStackResourceStatusReasons(credentialView, region, stackName, resourceErrorStatusesSet, cfClient);

        String cfEventsInString = cfEvents.stream().map(StackEvent::toString).collect(Collectors.joining("; "));

        if (!StringUtils.hasText(stackResourceStatusReasons)) {
            stackResourceStatusReasons = getStackEventResourceStatus(cfEvents, resourceErrorStatusesSet);
        }

        String errorReason = Stream.of(stackStatusReason, stackResourceStatusReasons).filter(Objects::nonNull).collect(Collectors.joining(" "));
        LOGGER.debug("Cloudformation stack {} has the following error reason: {}. Events: {}", stackName, errorReason, cfEventsInString);
        return errorReason;
    }

    private List<StackEvent> getCfEvents(String stackName, AmazonCloudFormationClient cfClient) {
        DescribeStackEventsResult eventsResult = cfClient.describeStackEvents(new DescribeStackEventsRequest().withStackName(stackName));
        return eventsResult.getStackEvents();
    }

    private String getStackResourceStatusReasons(AwsCredentialView credentialView, String region, String stackName, Set<String> resourceErrorStatuses,
            AmazonCloudFormationClient cfClient) {
        DescribeStackResourcesRequest describeStackResourcesRequest = new DescribeStackResourcesRequest().withStackName(stackName);
        DescribeStackResourcesResult describeStackResourcesResult = cfClient.describeStackResources(describeStackResourcesRequest);
        String stackResourceStatusReasons = describeStackResourcesResult.getStackResources().stream()
                .filter(stackResource -> resourceErrorStatuses.contains(stackResource.getResourceStatus()))
                .map(stackResource -> getStackResourceMessage(credentialView, region, stackResource))
                .collect(Collectors.joining(", "));
        return stackResourceStatusReasons;
    }

    private String getStackEventResourceStatus(List<StackEvent> cfEvents, Set<String> resourceErrorStatuses) {
        String stackResourceStatusReasons = cfEvents.stream()
                .filter(event -> resourceErrorStatuses.contains(event.getResourceStatus()))
                .map(StackEvent::getResourceStatusReason)
                .collect(Collectors.joining(", "));
        return stackResourceStatusReasons;
    }

    private String getStackStatusReason(String stackName, AmazonCloudFormationClient cfClient) {
        DescribeStacksRequest describeStacksRequest = new DescribeStacksRequest().withStackName(stackName);
        DescribeStacksResult describeStacksResult = cfClient.describeStacks(describeStacksRequest);
        LOGGER.debug("Cloudformation stack {} describe result: {}", stackName, describeStacksResult);
        String stackStatusReason = null;
        if (!CollectionUtils.isEmpty(describeStacksResult.getStacks())) {
            stackStatusReason = describeStacksResult.getStacks().get(0).getStackStatusReason();
        }
        LOGGER.debug("Cloudformation stack {} has the error status reason: {}", stackName, stackStatusReason);
        return stackStatusReason;
    }

    private String getStackResourceMessage(AwsCredentialView credentialView, String region, StackResource stackResource) {
        String resourceId = stackResource.getLogicalResourceId();
        String statusReason = awsEncodedAuthorizationFailureMessageDecoder
                .decodeAuthorizationFailureMessageIfNeeded(credentialView, region, stackResource.getResourceStatusReason());
        LOGGER.debug("Cloudformation resource {} has the error status reason: {}", resourceId, statusReason);
        return resourceId + ": " + statusReason;
    }
}
