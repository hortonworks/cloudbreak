package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.alarm.AwsNativeCloudWatchService;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.cloudwatch.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.cloudwatch.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.Tag;
import software.amazon.awssdk.services.cloudwatch.model.TagResourceRequest;

@ExtendWith(MockitoExtension.class)
class CloudWatchTagUpdateStrategyTest {

    private static final String REGION_NAME = "regionName";

    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final String EXISTING_TAG_KEY = "existingTagKey";

    private static final String EXISTING_TAG_VALUE = "existingTagValue";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    private static final Map<String, String> EXISTING_TAGS = Map.of(EXISTING_TAG_KEY, EXISTING_TAG_VALUE);

    private static final List<Tag> CLOUD_WATCH_TAGS = List.of(Tag.builder()
            .key("custom")
            .value("value")
            .build());

    @Mock
    private AwsNativeCloudWatchService awsNativeCloudWatchService;

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AmazonCloudWatchClient cloudWatchClient;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private AwsTaggingService awsTaggingService;

    @InjectMocks
    private CloudWatchTagUpdateStrategy underTest;

    private CloudResource cloudResource;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn(REGION_NAME);
        cloudResource = CloudResource.builder()
                .withType(ResourceType.AWS_CLOUD_WATCH)
                .withName("resourceName")
                .withInstanceId(INSTANCE_ID)
                .withReference(null)
                .withParameters(Collections.emptyMap())
                .build();
    }

    @Test
    void testUpdateTagsForAwsCloudWatch() {
        String alarmArn = "alarmArn";
        MetricAlarm alarm = MetricAlarm.builder()
                .alarmArn(alarmArn)
                .build();

        when(commonAwsClient.createCloudWatchClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(cloudWatchClient);
        when(awsNativeCloudWatchService.getMetricAlarmsForInstances(eq(REGION_NAME), any(AwsCredentialView.class), eq(List.of(INSTANCE_ID))))
                .thenReturn(List.of(alarm));
        when(cloudWatchClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder()
                .tags(Tag.builder()
                        .key(EXISTING_TAG_KEY)
                        .value(EXISTING_TAG_VALUE)
                        .build())
                .build());
        when(awsTaggingService.prepareCloudWatchTags(USER_DEFINED_TAGS)).thenReturn(CLOUD_WATCH_TAGS);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(cloudWatchClient).tagResource(
                software.amazon.awssdk.services.cloudwatch.model.TagResourceRequest.builder()
                        .resourceARN(alarmArn)
                        .tags(CLOUD_WATCH_TAGS)
                        .build()
        );
    }

    @Test
    void testUpdateTagsForAwsCloudWatchWithMultipleAlarms() {
        String alarmArn1 = "alarmArn1";
        String alarmArn2 = "alarmArn2";
        MetricAlarm alarm1 = MetricAlarm.builder().alarmArn(alarmArn1).build();
        MetricAlarm alarm2 = MetricAlarm.builder().alarmArn(alarmArn2).build();

        when(commonAwsClient.createCloudWatchClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(cloudWatchClient);
        when(awsNativeCloudWatchService.getMetricAlarmsForInstances(any(), any(), any()))
                .thenReturn(List.of(alarm1, alarm2));
        when(cloudWatchClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder()
                .tags(Tag.builder()
                        .key(EXISTING_TAG_KEY)
                        .value(EXISTING_TAG_VALUE)
                        .build())
                .build());
        when(awsTaggingService.prepareCloudWatchTags(USER_DEFINED_TAGS)).thenReturn(CLOUD_WATCH_TAGS);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(cloudWatchClient).tagResource(
                TagResourceRequest.builder()
                        .resourceARN(alarmArn1)
                        .tags(CLOUD_WATCH_TAGS)
                        .build()
        );
        verify(cloudWatchClient).tagResource(
                TagResourceRequest.builder()
                        .resourceARN(alarmArn2)
                        .tags(CLOUD_WATCH_TAGS)
                        .build()
        );
    }

    @Test
    void testUpdateTagsSkipUpdateWhenTagsAlreadyUpToDate() {
        String alarmArn1 = "alarmArn1";
        String alarmArn2 = "alarmArn2";
        MetricAlarm alarm1 = MetricAlarm.builder().alarmArn(alarmArn1).build();
        MetricAlarm alarm2 = MetricAlarm.builder().alarmArn(alarmArn2).build();

        when(commonAwsClient.createCloudWatchClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(cloudWatchClient);
        when(awsNativeCloudWatchService.getMetricAlarmsForInstances(any(), any(), any()))
                .thenReturn(List.of(alarm1, alarm2));
        when(cloudWatchClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder()
                .tags(Tag.builder()
                        .key(EXISTING_TAG_KEY)
                        .value(EXISTING_TAG_VALUE)
                        .build())
                .build());

        underTest.updateTags(authenticatedContext, cloudResource, EXISTING_TAGS);

        verify(cloudWatchClient, times(0)).tagResource(any(TagResourceRequest.class));
    }

    @Test
    void testUpdateTagsForAwsCloudWatchWithNoAlarms() {
        when(commonAwsClient.createCloudWatchClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(cloudWatchClient);
        when(awsNativeCloudWatchService.getMetricAlarmsForInstances(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verifyNoInteractions(cloudWatchClient);
    }
}