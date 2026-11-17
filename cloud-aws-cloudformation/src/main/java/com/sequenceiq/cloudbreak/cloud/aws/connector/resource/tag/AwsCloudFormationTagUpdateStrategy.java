package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.tag;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.VALIDATION_ERROR;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.CancellableWaiterConfiguration.cancellableWaiterConfiguration;
import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.WaiterRunner.handleWaiterError;
import static com.sequenceiq.common.api.type.ResourceType.CLOUDFORMATION_STACK;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsCloudWatchService;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.StackCancellationCheck;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.TagResourceRequest;
import software.amazon.awssdk.services.cloudwatch.model.UntagResourceRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;

// Runs before the other strategies so the stack level tags are in place before the individual EC2 resources of the stack are tagged
@Service
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AwsCloudFormationTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudFormationTagUpdateStrategy.class);

    private static final int EC2_TAG_BATCH_SIZE = 1000;

    private static final String IN_PROGRESS_STATUS_SUFFIX = "_IN_PROGRESS";

    private static final String NO_UPDATES_MESSAGE = "No updates are to be performed";

    private static final String LAUNCH_TEMPLATE_RESOURCE_TYPE = "AWS::EC2::LaunchTemplate";

    private static final String AUTOSCALING_GROUP_RESOURCE_TYPE = "AWS::AutoScaling::AutoScalingGroup";

    @Inject
    private AwsCloudFormationClient awsCloudFormationClient;

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Inject
    private AwsCloudWatchService awsCloudWatchService;

    @Inject
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    @Inject
    private AwsPageCollector awsPageCollector;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(CLOUDFORMATION_STACK);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsCloudFormationClient.createCloudFormationClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()), regionName);
        String stackName = cloudResource.getName();

        Stack stack = describeStackOrNull(cloudFormationClient, stackName, "update");
        if (stack == null) {
            return;
        }
        failIfStackOperationInProgress(stack, stackName);

        Map<String, String> existingTags = stack.tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));

        // Children are reconciled even when the stack tags match, so a retry repairs them after a partially failed update
        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.info("Tags for CloudFormation stack {} are already up to date, skipping stack tag update.", stackName);
        } else {
            updateStackTags(cloudFormationClient, authenticatedContext, stack, stackName, mergeTags(existingTags, tags));
        }

        StackResourceIds stackResourceIds = resolveStackResourceIds(cloudFormationClient, stackName);
        updateLaunchTemplateAndInstanceTags(authenticatedContext, stackResourceIds.launchTemplateIds(), stackName, tags);
        updateAutoScalingGroupAndInstanceTags(authenticatedContext, stackResourceIds.autoScalingGroupNames(), stackName, tags);
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonCloudFormationClient cloudFormationClient = awsCloudFormationClient.createCloudFormationClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()), regionName);
        String stackName = cloudResource.getName();

        Stack stack = describeStackOrNull(cloudFormationClient, stackName, "deletion");
        if (stack == null) {
            return;
        }
        failIfStackOperationInProgress(stack, stackName);

        Map<String, String> existingTags = stack.tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));

        if (hasTagKeysToDelete(existingTags, tagKeys)) {
            Map<String, String> remainingTags = removeTagKeys(existingTags, tagKeys);
            logTagDeletion(LOGGER, stackName, tagKeys, existingTags, remainingTags.keySet());
            updateStackTags(cloudFormationClient, authenticatedContext, stack, stackName, remainingTags);
        } else {
            LOGGER.info("No tags to delete for CloudFormation stack {}, skipping stack tag update.", stackName);
        }

        StackResourceIds stackResourceIds = resolveStackResourceIds(cloudFormationClient, stackName);
        deleteLaunchTemplateAndInstanceTags(authenticatedContext, stackResourceIds.launchTemplateIds(), stackName, tagKeys);
        deleteAutoScalingGroupAndInstanceTags(authenticatedContext, stackResourceIds.autoScalingGroupNames(), stackName, tagKeys);
    }

    private void failIfStackOperationInProgress(Stack stack, String stackName) {
        if (StringUtils.endsWith(stack.stackStatusAsString(), IN_PROGRESS_STATUS_SUFFIX)) {
            throw new CloudConnectorException(String.format("CloudFormation stack %s is in %s state, its tags can only be changed after the ongoing "
                    + "operation finishes. Please retry later.", stackName, stack.stackStatusAsString()));
        }
    }

    private void updateStackTags(AmazonCloudFormationClient cloudFormationClient, AuthenticatedContext authenticatedContext,
            Stack stack, String stackName, Map<String, String> desiredTags) {
        Collection<Tag> cloudFormationTags = awsTaggingService.prepareCloudformationTags(authenticatedContext, desiredTags);

        try {
            cloudFormationClient.updateStack(
                    UpdateStackRequest.builder()
                            .stackName(stackName)
                            .usePreviousTemplate(true)
                            .parameters(stack.parameters()
                                    .stream()
                                    .map(p -> Parameter.builder()
                                            .parameterKey(p.parameterKey())
                                            .usePreviousValue(true)
                                            .build())
                                    .toList())
                            .tags(cloudFormationTags)
                            .capabilities(stack.capabilities())
                            .build()
            );
            waitForStackUpdate(cloudFormationClient, authenticatedContext, stackName);
        } catch (AwsServiceException e) {
            if (VALIDATION_ERROR.equals(e.awsErrorDetails().errorCode()) && StringUtils.contains(e.awsErrorDetails().errorMessage(), NO_UPDATES_MESSAGE)) {
                LOGGER.info("CloudFormation stack {} already has the desired tags, no stack update was needed.", stackName);
            } else {
                throw e;
            }
        }
    }

    private void waitForStackUpdate(AmazonCloudFormationClient cloudFormationClient, AuthenticatedContext authenticatedContext, String stackName) {
        StackCancellationCheck cancellationCheck = new StackCancellationCheck(authenticatedContext.getCloudContext().getId());
        try (CloudFormationWaiter waiter = cloudFormationClient.waiters()) {
            LOGGER.debug("Waiting for CloudFormation stack {} tag update", stackName);
            waiter.waitUntilStackUpdateComplete(DescribeStacksRequest.builder().stackName(stackName).build(),
                    cancellableWaiterConfiguration(cancellationCheck));
        } catch (Exception e) {
            handleWaiterError(String.format("CloudFormation stack %s tag update failed.", stackName),
                    () -> awsCloudFormationErrorMessageProvider.getErrorReason(authenticatedContext, stackName, ResourceStatus.UPDATE_FAILED), e);
        }
    }

    private Stack describeStackOrNull(AmazonCloudFormationClient cloudFormationClient, String stackName, String operation) {
        try {
            DescribeStacksResponse describeStacksResponse = cloudFormationClient.describeStacks(DescribeStacksRequest.builder()
                    .stackName(stackName)
                    .build());
            if (describeStacksResponse.stacks().isEmpty()) {
                LOGGER.warn("CloudFormation stack {} not found, skipping tag {}", stackName, operation);
                return null;
            }
            return describeStacksResponse.stacks().getFirst();
        } catch (AwsServiceException e) {
            if (e.awsErrorDetails().errorMessage().contains(stackName + " does not exist")) {
                LOGGER.warn("CloudFormation stack {} not found, skipping tag {}", stackName, operation);
                return null;
            }
            throw e;
        }
    }

    private void updateLaunchTemplateAndInstanceTags(AuthenticatedContext authenticatedContext,
            List<String> launchTemplateIdsOfStack, String stackName, Map<String, String> tags) {
        cascadeLaunchTemplateTags(authenticatedContext, launchTemplateIdsOfStack, stackName, "update", (ec2Client, launchTemplateIds) -> {
            Collection<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = awsTaggingService.prepareEc2Tags(tags);
            ec2Client.createTags(CreateTagsRequest.builder()
                    .resources(launchTemplateIds)
                    .tags(ec2Tags)
                    .build());
            LOGGER.debug("Updated tags for {} launch templates of stack {}", launchTemplateIds.size(), stackName);
        });
    }

    private void cascadeLaunchTemplateTags(AuthenticatedContext authenticatedContext, List<String> launchTemplateIds,
            String stackName, String operation, BiConsumer<AmazonEc2Client, List<String>> tagOperation) {
        if (launchTemplateIds.isEmpty()) {
            LOGGER.debug("No launch templates found for stack {}, skipping launch template tag {}.", stackName, operation);
        } else {
            AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);
            tagOperation.accept(ec2Client, launchTemplateIds);
        }
    }

    private StackResourceIds resolveStackResourceIds(AmazonCloudFormationClient cloudFormationClient, String stackName) {
        // ListStackResources is paged, DescribeStackResources would silently return only the first 100 resources of the stack
        List<StackResourceSummary> stackResources = awsPageCollector.collectPages(
                cloudFormationClient::listStackResources,
                ListStackResourcesRequest.builder()
                        .stackName(stackName)
                        .build(),
                ListStackResourcesResponse::stackResourceSummaries,
                ListStackResourcesResponse::nextToken,
                (request, nextToken) -> request.toBuilder().nextToken(nextToken).build());

        Map<String, List<String>> physicalResourceIdsByType = stackResources.stream()
                .filter(r -> LAUNCH_TEMPLATE_RESOURCE_TYPE.equals(r.resourceType()) || AUTOSCALING_GROUP_RESOURCE_TYPE.equals(r.resourceType()))
                .collect(Collectors.groupingBy(StackResourceSummary::resourceType,
                        Collectors.mapping(StackResourceSummary::physicalResourceId, Collectors.toList())));

        StackResourceIds stackResourceIds = new StackResourceIds(
                physicalResourceIdsByType.getOrDefault(LAUNCH_TEMPLATE_RESOURCE_TYPE, List.of()),
                physicalResourceIdsByType.getOrDefault(AUTOSCALING_GROUP_RESOURCE_TYPE, List.of()));
        LOGGER.debug("Resolved taggable child resources of stack {}: {}", stackName, stackResourceIds);
        return stackResourceIds;
    }

    private void updateAutoScalingGroupAndInstanceTags(AuthenticatedContext authenticatedContext,
            List<String> autoScalingGroupNames, String stackName, Map<String, String> tags) {
        cascadeAutoScalingGroupTags(authenticatedContext, autoScalingGroupNames, stackName, "update",
                (ec2Client, cloudWatchClient, asgNames, instanceIds) -> {
                    updateAutoScalingGroupInstanceTags(ec2Client, instanceIds, awsTaggingService.prepareEc2Tags(tags));
                    LOGGER.debug("Updated tags for {} existing instances across {} Auto Scaling Groups",
                            instanceIds.size(), asgNames.size());
                    updateAutoScalingGroupCloudWatchAlarmTags(cloudWatchClient, instanceIds, tags);
                });
    }

    private void cascadeAutoScalingGroupTags(AuthenticatedContext authenticatedContext, List<String> asgNames,
            String stackName, String operation, AutoScalingGroupInstanceTagOperation tagOperation) {
        if (asgNames.isEmpty()) {
            LOGGER.debug("No Auto Scaling Groups found for stack {}, skipping ASG tag {}.", stackName, operation);
        } else {
            String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
            AwsCredentialView awsCredential = new AwsCredentialView(authenticatedContext.getCloudCredential());
            AmazonAutoScalingClient asgClient = awsCloudFormationClient.createAutoScalingClient(awsCredential, regionName);
            AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);
            AmazonCloudWatchClient cloudWatchClient = commonAwsClient.createCloudWatchClient(awsCredential, regionName);

            List<String> allInstanceIds = collectAutoScalingGroupInstanceIds(asgClient, asgNames);
            if (!allInstanceIds.isEmpty()) {
                tagOperation.apply(ec2Client, cloudWatchClient, asgNames, allInstanceIds);
            }
        }
    }

    private List<String> collectAutoScalingGroupInstanceIds(AmazonAutoScalingClient asgClient, List<String> asgNames) {
        DescribeAutoScalingGroupsResponse describeResponse = asgClient.describeAutoScalingGroups(
                DescribeAutoScalingGroupsRequest.builder()
                        .autoScalingGroupNames(asgNames)
                        .build());

        return describeResponse.autoScalingGroups().stream()
                .flatMap(asg -> asg.instances().stream())
                .map(software.amazon.awssdk.services.autoscaling.model.Instance::instanceId)
                .toList();
    }

    private void updateAutoScalingGroupInstanceTags(AmazonEc2Client ec2Client,
            List<String> instanceIds, Collection<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags) {
        Lists.partition(instanceIds, EC2_TAG_BATCH_SIZE).forEach(batch ->
                ec2Client.createTags(CreateTagsRequest.builder()
                        .resources(batch)
                        .tags(ec2Tags)
                        .build()));
    }

    private void updateAutoScalingGroupCloudWatchAlarmTags(AmazonCloudWatchClient cloudWatchClient,
            List<String> instanceIds, Map<String, String> tags) {

        List<String> alarmArns = resolveAlarmArnsForInstances(cloudWatchClient, instanceIds);

        if (alarmArns.isEmpty()) {
            LOGGER.debug("No CloudWatch alarms found for {} instances, skipping alarm tag update.", instanceIds.size());
            return;
        }

        Collection<software.amazon.awssdk.services.cloudwatch.model.Tag> cloudWatchTags = awsTaggingService.prepareCloudWatchTags(tags);

        alarmArns.forEach(arn -> {
            cloudWatchClient.tagResource(TagResourceRequest.builder()
                    .resourceARN(arn)
                    .tags(cloudWatchTags)
                    .build());
            LOGGER.debug("Updated tags for CloudWatch alarm: {}", arn);
        });

        LOGGER.debug("Updated tags for {} CloudWatch alarms", alarmArns.size());
    }

    private void deleteAutoScalingGroupCloudWatchAlarmTags(AmazonCloudWatchClient cloudWatchClient,
            List<String> instanceIds, Set<String> tagKeys) {

        List<String> alarmArns = resolveAlarmArnsForInstances(cloudWatchClient, instanceIds);

        if (alarmArns.isEmpty()) {
            LOGGER.debug("No CloudWatch alarms found for {} instances, skipping alarm tag deletion.", instanceIds.size());
            return;
        }

        alarmArns.forEach(arn -> {
            cloudWatchClient.untagResource(UntagResourceRequest.builder()
                    .resourceARN(arn)
                    .tagKeys(tagKeys)
                    .build());
            LOGGER.debug("Deleted tags for CloudWatch alarm: {}", arn);
        });

        LOGGER.debug("Deleted tags for {} CloudWatch alarms", alarmArns.size());
    }

    private List<String> resolveAlarmArnsForInstances(AmazonCloudWatchClient cloudWatchClient, List<String> instanceIds) {
        return awsCloudWatchService.getSystemFailureAlarmsForInstances(cloudWatchClient, instanceIds).stream()
                .map(MetricAlarm::alarmArn)
                .toList();
    }

    private void deleteLaunchTemplateAndInstanceTags(AuthenticatedContext authenticatedContext,
            List<String> launchTemplateIdsOfStack, String stackName, Set<String> tagKeys) {
        cascadeLaunchTemplateTags(authenticatedContext, launchTemplateIdsOfStack, stackName, "deletion", (ec2Client, launchTemplateIds) -> {
            logTagKeyDeletion(LOGGER, String.format("launch templates %s of stack %s", launchTemplateIds, stackName), tagKeys);
            ec2Client.deleteTags(DeleteTagsRequest.builder()
                    .resources(launchTemplateIds)
                    .tags(toEc2TagKeys(tagKeys))
                    .build());
            LOGGER.debug("Deleted tags for {} launch templates of stack {}", launchTemplateIds.size(), stackName);
        });
    }

    private void deleteAutoScalingGroupAndInstanceTags(AuthenticatedContext authenticatedContext,
            List<String> autoScalingGroupNames, String stackName, Set<String> tagKeys) {
        cascadeAutoScalingGroupTags(authenticatedContext, autoScalingGroupNames, stackName, "deletion",
                (ec2Client, cloudWatchClient, asgNames, instanceIds) -> {
                    logTagKeyDeletion(LOGGER, String.format("instances %s of stack %s", instanceIds, stackName), tagKeys);
                    Collection<software.amazon.awssdk.services.ec2.model.Tag> ec2TagKeys = toEc2TagKeys(tagKeys);
                    Lists.partition(instanceIds, EC2_TAG_BATCH_SIZE).forEach(batch ->
                            ec2Client.deleteTags(DeleteTagsRequest.builder()
                                    .resources(batch)
                                    .tags(ec2TagKeys)
                                    .build()));
                    LOGGER.debug("Deleted tags for {} existing instances across {} Auto Scaling Groups",
                            instanceIds.size(), asgNames.size());
                    deleteAutoScalingGroupCloudWatchAlarmTags(cloudWatchClient, instanceIds, tagKeys);
                });
    }

    private Collection<software.amazon.awssdk.services.ec2.model.Tag> toEc2TagKeys(Set<String> tagKeys) {
        return tagKeys.stream()
                .map(key -> software.amazon.awssdk.services.ec2.model.Tag.builder().key(key).build())
                .toList();
    }

    private record StackResourceIds(List<String> launchTemplateIds, List<String> autoScalingGroupNames) {
    }

    @FunctionalInterface
    private interface AutoScalingGroupInstanceTagOperation {
        void apply(AmazonEc2Client ec2Client, AmazonCloudWatchClient cloudWatchClient, List<String> asgNames, List<String> instanceIds);
    }
}