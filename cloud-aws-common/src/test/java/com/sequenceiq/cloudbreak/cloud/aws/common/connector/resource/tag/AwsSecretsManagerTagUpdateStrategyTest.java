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
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonSecretsManagerClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.DescribeSecretResponse;
import software.amazon.awssdk.services.secretsmanager.model.Tag;
import software.amazon.awssdk.services.secretsmanager.model.TagResourceRequest;

@ExtendWith(MockitoExtension.class)
class AwsSecretsManagerTagUpdateStrategyTest {

    private static final String REGION_NAME = "regionName";

    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final String EXISTING_TAG_KEY = "existingTagKey";

    private static final String EXISTING_TAG_VALUE = "existingTagValue";

    private static final Map<String, String> EXISTING_TAGS = Map.of(EXISTING_TAG_KEY, EXISTING_TAG_VALUE);

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

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
    private AmazonSecretsManagerClient secretsManagerClient;

    @Mock
    private AwsTaggingService awsTaggingService;

    @InjectMocks
    private AwsSecretsManagerTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn(REGION_NAME);
    }

    @Test
    void testUpdateTagsForAwsSecretsManagerSecret() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_SECRETSMANAGER_SECRET, null, RESOURCE_REFERENCE);
        List<Tag> expectedTags = toSecretsTags(USER_DEFINED_TAGS);
        when(commonAwsClient.createSecretsManagerClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(secretsManagerClient);
        when(secretsManagerClient.describeSecret(any(DescribeSecretRequest.class))).thenReturn(DescribeSecretResponse.builder()
                        .arn(RESOURCE_REFERENCE)
                        .tags(Tag.builder()
                                .key(EXISTING_TAG_KEY)
                                .value(EXISTING_TAG_VALUE)
                                .build())
                .build());
        when(awsTaggingService.prepareSecretsManagerTags(USER_DEFINED_TAGS)).thenReturn(expectedTags);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(secretsManagerClient).tagResource(TagResourceRequest.builder()
                .secretId(RESOURCE_REFERENCE)
                .tags(expectedTags)
                .build());
    }

    @Test
    void testUpdateTagsSkipUpdateWhenTagsAlreadyUpToDate() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_SECRETSMANAGER_SECRET, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createSecretsManagerClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(secretsManagerClient);
        when(secretsManagerClient.describeSecret(any(DescribeSecretRequest.class))).thenReturn(DescribeSecretResponse.builder()
                .arn(RESOURCE_REFERENCE)
                .tags(Tag.builder()
                        .key(EXISTING_TAG_KEY)
                        .value(EXISTING_TAG_VALUE)
                        .build())
                .build());

        underTest.updateTags(authenticatedContext, cloudResource, EXISTING_TAGS);

        verify(secretsManagerClient, times(0)).tagResource(any(TagResourceRequest.class));
    }

    private List<Tag> toSecretsTags(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .key(e.getKey()).value(e.getValue()).build())
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