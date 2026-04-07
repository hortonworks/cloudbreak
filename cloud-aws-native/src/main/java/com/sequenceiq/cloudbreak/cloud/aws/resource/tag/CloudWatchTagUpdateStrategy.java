package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_CLOUD_WATCH;

import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm.AwsNativeCloudWatchService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.Tag;
import software.amazon.awssdk.services.cloudwatch.model.TagResourceRequest;

@Service
public class CloudWatchTagUpdateStrategy implements TagUpdateStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudWatchTagUpdateStrategy.class);

    @Inject
    private CommonAwsClient commonAwsClient;

    @Inject
    private AwsNativeCloudWatchService awsNativeCloudWatchService;

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

        List<Tag> cloudWatchTags = tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .key(e.getKey())
                        .value(e.getValue())
                        .build())
                .toList();

        alarms.forEach(alarm -> {
            try {
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
