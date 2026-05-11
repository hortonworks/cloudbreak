package com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonKmsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.kms.model.ListResourceTagsRequest;
import software.amazon.awssdk.services.kms.model.ListResourceTagsResponse;
import software.amazon.awssdk.services.kms.model.Tag;
import software.amazon.awssdk.services.kms.model.TagResourceRequest;

@ExtendWith(MockitoExtension.class)
class AwsKmsTagUpdateStrategyTest {

    private static final String REGION_NAME = "regionName";

    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final String EXISTING_TAG_KEY = "existingTagKey";

    private static final String EXISTING_TAG_VALUE = "existingTagValue";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    private static final Map<String, String> EXISTING_TAGS = Map.of(EXISTING_TAG_KEY, EXISTING_TAG_VALUE);

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
    private CommonAwsClient commonAwsClient;

    @Mock
    private AmazonKmsClient kmsClient;

    @Mock
    private AwsTaggingService awsTaggingService;

    @InjectMocks
    private AwsKmsTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn(REGION_NAME);
    }

    @Test
    void testUpdateTagsForAwsKmsKey() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_KMS_KEY, null, RESOURCE_REFERENCE);
        List<Tag> expectedTags = toKmsTags(USER_DEFINED_TAGS);
        when(commonAwsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(kmsClient);
        when(kmsClient.listResourceTags(any(ListResourceTagsRequest.class))).thenReturn(ListResourceTagsResponse.builder()
                        .tags(Tag.builder()
                                .tagKey(EXISTING_TAG_KEY)
                                .tagValue(EXISTING_TAG_VALUE)
                                .build())
                .build());
        when(awsTaggingService.prepareKmsTags(USER_DEFINED_TAGS)).thenReturn(expectedTags);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(kmsClient).tagResource(software.amazon.awssdk.services.kms.model.TagResourceRequest.builder()
                .keyId(RESOURCE_REFERENCE)
                .tags(expectedTags)
                .build());
    }

    @Test
    void testUpdateTagsSkipUpdateWhenTagsAlreadyUpToDate() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_KMS_KEY, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createAWSKMS(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(kmsClient);
        when(kmsClient.listResourceTags(any(ListResourceTagsRequest.class))).thenReturn(ListResourceTagsResponse.builder()
                .tags(Tag.builder()
                        .tagKey(EXISTING_TAG_KEY)
                        .tagValue(EXISTING_TAG_VALUE)
                        .build())
                .build());

        underTest.updateTags(authenticatedContext, cloudResource, EXISTING_TAGS);

        verify(kmsClient, times(0)).tagResource(any(TagResourceRequest.class));
    }

    private List<Tag> toKmsTags(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .tagKey(e.getKey())
                        .tagValue(e.getValue())
                        .build())
                .toList();
    }

    private CloudResource buildResource(ResourceType type, String instanceId, String reference) {
        return CloudResource.builder()
                .withType(type)
                .withName(type.name().toLowerCase())
                .withInstanceId(instanceId)
                .withReference(reference)
                .withParameters(Collections.emptyMap())
                .build();
    }
}