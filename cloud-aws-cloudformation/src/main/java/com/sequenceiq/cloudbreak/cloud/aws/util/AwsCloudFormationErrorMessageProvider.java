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

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@Component
public class AwsCloudFormationErrorMessageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudFormationErrorMessageProvider.class);

    @Inject
    private AwsCloudFormationClient awsClient;

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

        Set<ResourceStatus> resourceErrorStatusesSet = Arrays.stream(resourceErrorStatuses).collect(Collectors.toSet());

        String stackResourceStatusReasons = getStackResourceStatusReasons(credentialView, region, stackName, resourceErrorStatusesSet, cfClient);

        String cfEventsInString = cfEvents.stream().map(StackEvent::toString).collect(Collectors.joining("; "));

        if (!StringUtils.hasText(stackResourceStatusReasons)) {
            stackResourceStatusReasons = getStackEventResourceStatus(cfEvents, resourceErrorStatusesSet);
        }

        String errorReason = Stream.of(stackStatusReason, stackResourceStatusReasons).filter(Objects::nonNull).collect(Collectors.joining(" "));
        LOGGER.warn("Cloudformation stack {} has the following error reason: {}. Events: {}", stackName, errorReason, cfEventsInString);
        return errorReason;
    }

    private List<StackEvent> getCfEvents(String stackName, AmazonCloudFormationClient cfClient) {
        DescribeStackEventsResponse eventsResponse = cfClient.describeStackEvents(DescribeStackEventsRequest.builder().stackName(stackName).build());
        return eventsResponse.stackEvents();
    }

    private String getStackResourceStatusReasons(AwsCredentialView credentialView, String region, String stackName, Set<ResourceStatus> resourceErrorStatuses,
            AmazonCloudFormationClient cfClient) {
        DescribeStackResourcesRequest describeStackResourcesRequest = DescribeStackResourcesRequest.builder().stackName(stackName).build();
        DescribeStackResourcesResponse describeStackResourcesResponse = cfClient.describeStackResources(describeStackResourcesRequest);
        String stackResourceStatusReasons = describeStackResourcesResponse.stackResources().stream()
                .filter(stackResource -> resourceErrorStatuses.contains(stackResource.resourceStatus()))
                .map(stackResource -> getStackResourceMessage(credentialView, region, stackResource))
                .collect(Collectors.joining(", "));
        return stackResourceStatusReasons;
    }

    private String getStackEventResourceStatus(List<StackEvent> cfEvents, Set<ResourceStatus> resourceErrorStatuses) {
        String stackResourceStatusReasons = cfEvents.stream()
                .filter(event -> resourceErrorStatuses.contains(event.resourceStatus()))
                .map(StackEvent::resourceStatusReason)
                .collect(Collectors.joining(", "));
        return stackResourceStatusReasons;
    }

    private String getStackStatusReason(String stackName, AmazonCloudFormationClient cfClient) {
        DescribeStacksRequest describeStacksRequest = DescribeStacksRequest.builder().stackName(stackName).build();
        DescribeStacksResponse describeStacksResponse = cfClient.describeStacks(describeStacksRequest);
        LOGGER.debug("Cloudformation stack {} describe result: {}", stackName, describeStacksResponse);
        String stackStatusReason = null;
        if (!CollectionUtils.isEmpty(describeStacksResponse.stacks())) {
            stackStatusReason = describeStacksResponse.stacks().get(0).stackStatusReason();
        }
        LOGGER.warn("Cloudformation stack {} has the error status reason: {}", stackName, stackStatusReason);
        return stackStatusReason;
    }

    private String getStackResourceMessage(AwsCredentialView credentialView, String region, StackResource stackResource) {
        String resourceId = stackResource.logicalResourceId();
        String statusReason = awsEncodedAuthorizationFailureMessageDecoder
                .decodeAuthorizationFailureMessageIfNeeded(credentialView, region, stackResource.resourceStatusReason());
        LOGGER.warn("Cloudformation resource {} has the error status reason: {}", resourceId, statusReason);
        return resourceId + ": " + statusReason;
    }
}
