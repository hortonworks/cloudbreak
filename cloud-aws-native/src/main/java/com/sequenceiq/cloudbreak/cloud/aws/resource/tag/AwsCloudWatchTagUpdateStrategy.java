package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_CLOUD_WATCH;

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
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm.AwsNativeCloudWatchService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.cloudwatch.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.Tag;
import software.amazon.awssdk.services.cloudwatch.model.TagResourceRequest;
import software.amazon.awssdk.services.cloudwatch.model.UntagResourceRequest;

@Service
public class AwsCloudWatchTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsCloudWatchTagUpdateStrategy.class);

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsNativeCloudWatchService awsNativeCloudWatchService;

    @Inject
    private AwsTaggingService awsTaggingService;

    @Override
    public Set<ResourceType> supportedTypes() {
        return Set.of(AWS_CLOUD_WATCH);
    }

    @Override
    public void updateTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Map<String, String> tags) {
        forEachAlarm(authenticatedContext, cloudResource.getInstanceId(),
                (cloudWatchClient, alarm) -> updateAlarmTags(cloudWatchClient, alarm, tags));
    }

    @Override
    public void deleteTags(AuthenticatedContext authenticatedContext, CloudResource cloudResource, Set<String> tagKeys) {
        forEachAlarm(authenticatedContext, cloudResource.getInstanceId(),
                (cloudWatchClient, alarm) -> deleteAlarmTags(cloudWatchClient, alarm, tagKeys));
    }

    private void forEachAlarm(AuthenticatedContext authenticatedContext, String instanceId,
            BiConsumer<AmazonCloudWatchClient, MetricAlarm> alarmTagOperation) {
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        AwsCredentialView credentialView = new AwsCredentialView(authenticatedContext.getCloudCredential());
        AmazonCloudWatchClient cloudWatchClient = commonAwsClient.createCloudWatchClient(credentialView, regionName);

        List<MetricAlarm> alarms = awsNativeCloudWatchService.getMetricAlarmsForInstances(regionName, credentialView, List.of(instanceId));
        alarms.forEach(alarm -> alarmTagOperation.accept(cloudWatchClient, alarm));
    }

    private void updateAlarmTags(AmazonCloudWatchClient cloudWatchClient, MetricAlarm alarm, Map<String, String> tags) {
        Map<String, String> existingTags = existingTags(cloudWatchClient, alarm);
        if (tagsAlreadyUpToDate(existingTags, tags)) {
            LOGGER.info("Tags for CloudWatch alarm {} are already up to date, skipping update.", alarm.alarmArn());
        } else {
            try {
                cloudWatchClient.tagResource(TagResourceRequest.builder()
                        .resourceARN(alarm.alarmArn())
                        .tags(awsTaggingService.prepareCloudWatchTags(tags))
                        .build());
            } catch (Exception e) {
                LOGGER.warn("Failed to update tags for Cloudwatch alarm {}: {}", alarm.alarmArn(), e.getMessage(), e);
            }
        }
    }

    private void deleteAlarmTags(AmazonCloudWatchClient cloudWatchClient, MetricAlarm alarm, Set<String> tagKeys) {
        Map<String, String> existingTags = existingTags(cloudWatchClient, alarm);
        if (hasTagKeysToDelete(existingTags, tagKeys)) {
            Map<String, String> remainingTags = removeTagKeys(existingTags, tagKeys);
            logTagDeletion(LOGGER, alarm.alarmArn(), tagKeys, existingTags, remainingTags.keySet());
            try {
                cloudWatchClient.untagResource(UntagResourceRequest.builder()
                        .resourceARN(alarm.alarmArn())
                        .tagKeys(tagKeys)
                        .build());
            } catch (Exception e) {
                LOGGER.warn("Failed to delete tags for Cloudwatch alarm {}: {}", alarm.alarmArn(), e.getMessage(), e);
            }
        } else {
            LOGGER.info("No tags to delete for CloudWatch alarm {}, skipping.", alarm.alarmArn());
        }
    }

    private Map<String, String> existingTags(AmazonCloudWatchClient cloudWatchClient, MetricAlarm alarm) {
        return cloudWatchClient.listTagsForResource(ListTagsForResourceRequest.builder()
                        .resourceARN(alarm.alarmArn())
                        .build())
                .tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value));
    }
}
