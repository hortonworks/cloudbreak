package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_CLOUD_WATCH;

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
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm.AwsNativeCloudWatchService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.cloudwatch.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.Tag;
import software.amazon.awssdk.services.cloudwatch.model.TagResourceRequest;

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
        String regionName = authenticatedContext.getCloudContext().getLocation().getRegion().getRegionName();
        CloudCredential cloudCredential = authenticatedContext.getCloudCredential();
        AmazonCloudWatchClient cloudWatchClient = commonAwsClient.createCloudWatchClient(new AwsCredentialView(cloudCredential), regionName);

        List<MetricAlarm> alarms = awsNativeCloudWatchService.getMetricAlarmsForInstances(regionName,
                new AwsCredentialView(cloudCredential), List.of(cloudResource.getInstanceId()));

        alarms.forEach(alarm -> {
            Map<String, String> existingTags = cloudWatchClient.listTagsForResource(
                            ListTagsForResourceRequest.builder()
                                    .resourceARN(alarm.alarmArn())
                                    .build())
                    .tags().stream()
                    .collect(Collectors.toMap(Tag::key, Tag::value));
            if (tagsAlreadyUpToDate(existingTags, tags)) {
                LOGGER.info("Tags for CloudWatch alarm {} are already up to date, skipping update.", alarm.alarmArn());
                return;
            }
            try {
                Collection<Tag> cloudWatchTags = awsTaggingService.prepareCloudWatchTags(tags);
                cloudWatchClient.tagResource(
                        TagResourceRequest.builder()
                                .resourceARN(alarm.alarmArn())
                                .tags(cloudWatchTags)
                                .build()
                );
            } catch (Exception e) {
                LOGGER.warn("Failed to update tags for Cloudwatch alarm {}: {}", alarm.alarmArn(), e.getMessage(), e);
            }
        });
    }
}
