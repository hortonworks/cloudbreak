package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.CLOUDFORMATION_STACK;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
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
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
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
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsForMetricRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsForMetricResponse;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.TagResourceRequest;
import software.amazon.awssdk.services.cloudwatch.model.UntagResourceRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;

@Service
public class AwsCloudFormationTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudFormationTagUpdateStrategy.class);

    private static final List<String> EC2_ALARM_METRICS = List.of(
            "CPUUtilization",
            "StatusCheckFailed",
            "StatusCheckFailed_Instance",
            "StatusCheckFailed_System",
            "DiskReadOps",
            "DiskWriteOps",
            "NetworkIn",
            "NetworkOut"
    );

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

        updateStackTags(cloudFormationClient, authenticatedContext, stack, stackName, mergeTags(existingTags, tags));

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
            logTagDeletion(LOGGER, stackName, tagKeys, existingTags, remainingTags.keySet());
            updateStackTags(cloudFormationClient, authenticatedContext, stack, stackName, remainingTags);
        } else {
            LOGGER.info("No tags to delete for CloudFormation stack {}, skipping stack tag update.", stackName);
        }

        deleteLaunchTemplateAndInstanceTags(authenticatedContext, cloudFormationClient, stackName, tagKeys);
        deleteAutoScalingGroupAndInstanceTags(authenticatedContext, cloudFormationClient, stackName, tagKeys);
    }

    private void updateStackTags(AmazonCloudFormationClient cloudFormationClient, AuthenticatedContext authenticatedContext,
            Stack stack, String stackName, Map<String, String> desiredTags) {
        Collection<Tag> cloudFormationTags = awsTaggingService.prepareCloudformationTags(authenticatedContext, desiredTags);

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
        cascadeLaunchTemplateTags(authenticatedContext, cloudFormationClient, stackName, "update", (ec2Client, launchTemplateIds) -> {
            Collection<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = awsTaggingService.prepareEc2Tags(tags);
            ec2Client.createTags(CreateTagsRequest.builder()
                    .resources(launchTemplateIds)
                    .tags(ec2Tags)
                    .build());
            LOGGER.debug("Updated tags for {} launch templates of stack {}", launchTemplateIds.size(), stackName);
        });
    }

    private void cascadeLaunchTemplateTags(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient cloudFormationClient,
            String stackName, String operation, BiConsumer<AmazonEc2Client, List<String>> tagOperation) {
        List<String> launchTemplateIds = resolveLaunchTemplateIds(cloudFormationClient, stackName);
        if (launchTemplateIds.isEmpty()) {
            LOGGER.debug("No launch templates found for stack {}, skipping launch template tag {}.", stackName, operation);
        } else {
            AmazonEc2Client ec2Client = commonAwsClient.createEc2Client(authenticatedContext);
            tagOperation.accept(ec2Client, launchTemplateIds);
        }
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
        cascadeAutoScalingGroupTags(authenticatedContext, cloudFormationClient, stackName, "update",
                (ec2Client, cloudWatchClient, asgNames, instanceIds) -> {
                    updateAutoScalingGroupInstanceTags(ec2Client, instanceIds, awsTaggingService.prepareEc2Tags(tags));
                    LOGGER.debug("Updated tags for {} existing instances across {} Auto Scaling Groups",
                            instanceIds.size(), asgNames.size());
                    updateAutoScalingGroupCloudWatchAlarmTags(cloudWatchClient, instanceIds, tags);
                });
    }

    private void cascadeAutoScalingGroupTags(AuthenticatedContext authenticatedContext, AmazonCloudFormationClient cloudFormationClient,
            String stackName, String operation, AutoScalingGroupInstanceTagOperation tagOperation) {
        List<String> asgNames = resolveAutoScalingGroupNames(cloudFormationClient, stackName);
        if (asgNames.isEmpty()) {
            LOGGER.debug("No Auto Scaling Groups found for stack {}, skipping ASG tag {}.", stackName, operation);
            return;
        }

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

    private List<String> resolveAlarmArnsForInstances(AmazonCloudWatchClient cloudWatchClient,
            List<String> instanceIds) {
        return instanceIds.stream()
            .flatMap(instanceId -> EC2_ALARM_METRICS.stream()
                .flatMap(metric -> {
                    DescribeAlarmsForMetricResponse response = cloudWatchClient.describeAlarmsForMetric(
                        DescribeAlarmsForMetricRequest.builder()
                            .namespace("AWS/EC2")
                            .metricName(metric)
                            .dimensions(Dimension.builder()
                                .name("InstanceId")
                                .value(instanceId)
                                .build())
                            .build());
                        return response.metricAlarms().stream()
                                .map(MetricAlarm::alarmArn);
                }))
            .distinct()
            .toList();
    }

    private void deleteLaunchTemplateAndInstanceTags(AuthenticatedContext authenticatedContext,
            AmazonCloudFormationClient cloudFormationClient, String stackName, Set<String> tagKeys) {
        cascadeLaunchTemplateTags(authenticatedContext, cloudFormationClient, stackName, "deletion", (ec2Client, launchTemplateIds) -> {
            logTagKeyDeletion(LOGGER, String.format("launch templates %s of stack %s", launchTemplateIds, stackName), tagKeys);
            ec2Client.deleteTags(DeleteTagsRequest.builder()
                    .resources(launchTemplateIds)
                    .tags(toEc2TagKeys(tagKeys))
                    .build());
            LOGGER.debug("Deleted tags for {} launch templates of stack {}", launchTemplateIds.size(), stackName);
        });
    }

    private void deleteAutoScalingGroupAndInstanceTags(AuthenticatedContext authenticatedContext,
            AmazonCloudFormationClient cloudFormationClient, String stackName, Set<String> tagKeys) {
        cascadeAutoScalingGroupTags(authenticatedContext, cloudFormationClient, stackName, "deletion",
                (ec2Client, cloudWatchClient, asgNames, instanceIds) -> {
                    logTagKeyDeletion(LOGGER, String.format("instances %s of stack %s", instanceIds, stackName), tagKeys);
                    ec2Client.deleteTags(DeleteTagsRequest.builder()
                            .resources(instanceIds)
                            .tags(toEc2TagKeys(tagKeys))
                            .build());
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

    @FunctionalInterface
    private interface AutoScalingGroupInstanceTagOperation {
        void apply(AmazonEc2Client ec2Client, AmazonCloudWatchClient cloudWatchClient, List<String> asgNames, List<String> instanceIds);
    }
}