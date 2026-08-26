package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.CLOUDFORMATION_STACK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackResource;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;

@Service
public class AwsCloudFormationTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudFormationTagUpdateStrategy.class);

    private static final String LAUNCH_TEMPLATE_RESOURCE_TYPE = "AWS::EC2::LaunchTemplate";

    private static final String AUTOSCALING_GROUP_RESOURCE_TYPE = "AWS::AutoScaling::AutoScalingGroup";

    @Inject
    private AwsCloudFormationClient awsCloudFormationClient;

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsTaggingService awsTaggingService;

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

        Map<String, String> existingTags = stack.tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));

        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.info("Tags for CloudFormation stack {} are already up to date, skipping update.", stackName);
            return;
        }

        Collection<Tag> cloudFormationTags = awsTaggingService.prepareCloudformationTags(authenticatedContext, mergeTags(existingTags, tags));

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

        updateLaunchTemplateAndInstanceTags(authenticatedContext, cloudFormationClient, stackName, tags);
        updateAutoScalingGroupAndInstanceTags(authenticatedContext, cloudFormationClient, stackName, tags);
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

        Map<String, String> existingTags = stack.tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));

        if (hasTagKeysToDelete(existingTags, tagKeys)) {
            Map<String, String> remainingTags = removeTagKeys(existingTags, tagKeys);
            Collection<Tag> cloudFormationTags = awsTaggingService.prepareCloudformationTags(authenticatedContext, remainingTags);

            logTagDeletion(LOGGER, stackName, tagKeys, existingTags, remainingTags.keySet());

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
        } else {
            LOGGER.info("No tags to delete for CloudFormation stack {}, skipping stack tag update.", stackName);
        }

        deleteLaunchTemplateAndInstanceTags(authenticatedContext, cloudFormationClient, stackName, tagKeys);
        deleteAutoScalingGroupAndInstanceTags(authenticatedContext, cloudFormationClient, stackName, tagKeys);
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
            AmazonCloudFormationClient cloudFormationClient, String stackName, Map<String, String> tags) {
        List<String> launchTemplateIds = resolveLaunchTemplateIds(cloudFormationClient, stackName);

        if (launchTemplateIds.isEmpty()) {
            LOGGER.debug("No launch templates found for stack {}, skipping launch template and instance tag update.", stackName);
            return;
        }

        AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);
        Collection<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = awsTaggingService.prepareEc2Tags(tags);

        ec2Client.createTags(CreateTagsRequest.builder()
                .resources(launchTemplateIds)
                .tags(ec2Tags)
                .build());
        LOGGER.debug("Updated tags for {} launch templates of stack {}", launchTemplateIds.size(), stackName);
    }

    private List<String> resolveLaunchTemplateIds(AmazonCloudFormationClient cloudFormationClient, String stackName) {
        DescribeStackResourcesResponse response = cloudFormationClient.describeStackResources(
                DescribeStackResourcesRequest.builder()
                        .stackName(stackName)
                        .build());

        return response.stackResources().stream()
                .filter(r -> LAUNCH_TEMPLATE_RESOURCE_TYPE.equals(r.resourceType()))
                .map(StackResource::physicalResourceId)
                .toList();
    }

    private void updateAutoScalingGroupAndInstanceTags(AuthenticatedContext authenticatedContext,
            AmazonCloudFormationClient cloudFormationClient, String stackName, Map<String, String> tags) {

        List<String> asgNames = resolveAutoScalingGroupNames(cloudFormationClient, stackName);
        if (asgNames.isEmpty()) {
            LOGGER.debug("No Auto Scaling Groups found for stack {}, skipping ASG tag update.", stackName);
            return;
        }

        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonAutoScalingClient asgClient = awsCloudFormationClient.createAutoScalingClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()), regionName);
        AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);
        Collection<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = awsTaggingService.prepareEc2Tags(tags);

        DescribeAutoScalingGroupsResponse describeResponse = asgClient.describeAutoScalingGroups(
                DescribeAutoScalingGroupsRequest.builder()
                        .autoScalingGroupNames(asgNames)
                        .build());

        List<String> allInstanceIds = new ArrayList<>();

        for (AutoScalingGroup asg : describeResponse.autoScalingGroups()) {
            List<String> instanceIds = asg.instances().stream()
                    .map(software.amazon.awssdk.services.autoscaling.model.Instance::instanceId)
                    .toList();
            allInstanceIds.addAll(instanceIds);
        }

        if (!allInstanceIds.isEmpty()) {
            updateAutoScalingGroupInstanceTags(ec2Client, allInstanceIds, ec2Tags);
            LOGGER.debug("Updated tags for {} existing instances across {} Auto Scaling Groups",
                    allInstanceIds.size(), asgNames.size());
        }
    }

    private List<String> resolveAutoScalingGroupNames(AmazonCloudFormationClient cloudFormationClient, String stackName) {
        DescribeStackResourcesResponse response = cloudFormationClient.describeStackResources(
                DescribeStackResourcesRequest.builder()
                        .stackName(stackName)
                        .build());

        return response.stackResources().stream()
                .filter(r -> AUTOSCALING_GROUP_RESOURCE_TYPE.equals(r.resourceType()))
                .map(StackResource::physicalResourceId)
                .toList();
    }

    private void updateAutoScalingGroupInstanceTags(AmazonEc2Client ec2Client,
            List<String> instanceIds, Collection<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags) {
        ec2Client.createTags(CreateTagsRequest.builder()
                .resources(instanceIds)
                .tags(ec2Tags)
                .build());
    }

    private void deleteLaunchTemplateAndInstanceTags(AuthenticatedContext authenticatedContext,
            AmazonCloudFormationClient cloudFormationClient, String stackName, Set<String> tagKeys) {
        List<String> launchTemplateIds = resolveLaunchTemplateIds(cloudFormationClient, stackName);

        if (launchTemplateIds.isEmpty()) {
            LOGGER.debug("No launch templates found for stack {}, skipping launch template tag deletion.", stackName);
            return;
        }

        AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);
        Collection<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = tagKeys.stream()
                .map(key -> software.amazon.awssdk.services.ec2.model.Tag.builder().key(key).build())
                .toList();

        logTagKeyDeletion(LOGGER, String.format("launch templates %s of stack %s", launchTemplateIds, stackName), tagKeys);

        ec2Client.deleteTags(DeleteTagsRequest.builder()
                .resources(launchTemplateIds)
                .tags(ec2Tags)
                .build());
        LOGGER.debug("Deleted tags for {} launch templates of stack {}", launchTemplateIds.size(), stackName);
    }

    private void deleteAutoScalingGroupAndInstanceTags(AuthenticatedContext authenticatedContext,
            AmazonCloudFormationClient cloudFormationClient, String stackName, Set<String> tagKeys) {
        List<String> asgNames = resolveAutoScalingGroupNames(cloudFormationClient, stackName);
        if (asgNames.isEmpty()) {
            LOGGER.debug("No Auto Scaling Groups found for stack {}, skipping ASG tag deletion.", stackName);
            return;
        }

        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AmazonAutoScalingClient asgClient = awsCloudFormationClient.createAutoScalingClient(
                new AwsCredentialView(authenticatedContext.getCloudCredential()), regionName);
        AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);
        Collection<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = tagKeys.stream()
                .map(key -> software.amazon.awssdk.services.ec2.model.Tag.builder().key(key).build())
                .toList();

        DescribeAutoScalingGroupsResponse describeResponse = asgClient.describeAutoScalingGroups(
                DescribeAutoScalingGroupsRequest.builder()
                        .autoScalingGroupNames(asgNames)
                        .build());

        List<String> allInstanceIds = new ArrayList<>();

        for (AutoScalingGroup asg : describeResponse.autoScalingGroups()) {
            List<String> instanceIds = asg.instances().stream()
                    .map(software.amazon.awssdk.services.autoscaling.model.Instance::instanceId)
                    .toList();
            allInstanceIds.addAll(instanceIds);
        }

        if (!allInstanceIds.isEmpty()) {
            logTagKeyDeletion(LOGGER, String.format("instances %s of stack %s", allInstanceIds, stackName), tagKeys);
            ec2Client.deleteTags(DeleteTagsRequest.builder()
                    .resources(allInstanceIds)
                    .tags(ec2Tags)
                    .build());
            LOGGER.debug("Deleted tags for {} existing instances across {} Auto Scaling Groups",
                    allInstanceIds.size(), asgNames.size());
        }
    }
}
