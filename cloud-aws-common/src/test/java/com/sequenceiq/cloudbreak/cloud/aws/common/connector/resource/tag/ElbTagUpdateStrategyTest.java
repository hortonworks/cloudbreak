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
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.elasticloadbalancingv2.model.AddTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.DescribeTagsResponse;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.Tag;
import software.amazon.awssdk.services.elasticloadbalancingv2.model.TagDescription;

@ExtendWith(MockitoExtension.class)
class ElbTagUpdateStrategyTest {

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
    private AmazonElasticLoadBalancingClient elbClient;

    @Mock
    private AwsTaggingService awsTaggingService;

    @InjectMocks
    private ElbTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn(REGION_NAME);
    }

    @Test
    void testUpdateTagsForElasticLoadBalancer() {
        CloudResource cloudResource = buildResource(ResourceType.ELASTIC_LOAD_BALANCER, null, RESOURCE_REFERENCE);
        List<Tag> expectedTags = toElbTags(USER_DEFINED_TAGS);
        when(commonAwsClient.createElasticLoadBalancingClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(elbClient);
        when(elbClient.describeTags(any(DescribeTagsRequest.class))).thenReturn(DescribeTagsResponse.builder()
                        .tagDescriptions(TagDescription.builder()
                                .tags(Tag.builder()
                                        .key(EXISTING_TAG_KEY)
                                        .value(EXISTING_TAG_VALUE)
                                        .build())
                                .resourceArn(RESOURCE_REFERENCE)
                                .build())
                .build());
        when(awsTaggingService.prepareElasticLoadBalancingTags(USER_DEFINED_TAGS)).thenReturn(expectedTags);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(elbClient).addTags(AddTagsRequest.builder()
                .resourceArns(RESOURCE_REFERENCE)
                .tags(expectedTags)
                .build());
    }

    @Test
    void testUpdateTagsSkipUpdateWhenTagsAlreadyUpToDate() {
        CloudResource cloudResource = buildResource(ResourceType.ELASTIC_LOAD_BALANCER, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createElasticLoadBalancingClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(elbClient);
        when(elbClient.describeTags(any(DescribeTagsRequest.class))).thenReturn(DescribeTagsResponse.builder()
                .tagDescriptions(TagDescription.builder()
                        .tags(Tag.builder()
                                .key(EXISTING_TAG_KEY)
                                .value(EXISTING_TAG_VALUE)
                                .build())
                        .resourceArn(RESOURCE_REFERENCE)
                        .build())
                .build());

        underTest.updateTags(authenticatedContext, cloudResource, EXISTING_TAGS);

        verify(elbClient, times(0)).addTags(any(AddTagsRequest.class));
    }

    @Test
    void testUpdateTagsForElasticLoadBalancerListener() {
        CloudResource cloudResource = buildResource(ResourceType.ELASTIC_LOAD_BALANCER_LISTENER, null, RESOURCE_REFERENCE);
        List<Tag> expectedTags = toElbTags(USER_DEFINED_TAGS);
        when(commonAwsClient.createElasticLoadBalancingClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(elbClient);
        when(elbClient.describeTags(any(DescribeTagsRequest.class))).thenReturn(DescribeTagsResponse.builder()
                .tagDescriptions(TagDescription.builder()
                        .tags(Tag.builder()
                                .key(EXISTING_TAG_KEY)
                                .value(EXISTING_TAG_VALUE)
                                .build())
                        .resourceArn(RESOURCE_REFERENCE)
                        .build())
                .build());
        when(awsTaggingService.prepareElasticLoadBalancingTags(USER_DEFINED_TAGS)).thenReturn(expectedTags);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(elbClient).addTags(AddTagsRequest.builder()
                .resourceArns(RESOURCE_REFERENCE)
                .tags(expectedTags)
                .build());
    }

    @Test
    void testUpdateTagsForElasticLoadBalancerTargetGroup() {
        CloudResource cloudResource = buildResource(ResourceType.ELASTIC_LOAD_BALANCER_TARGET_GROUP, null, RESOURCE_REFERENCE);
        List<Tag> expectedTags = toElbTags(USER_DEFINED_TAGS);
        when(commonAwsClient.createElasticLoadBalancingClient(any(AwsCredentialView.class), eq(REGION_NAME))).thenReturn(elbClient);
        when(elbClient.describeTags(any(DescribeTagsRequest.class))).thenReturn(DescribeTagsResponse.builder()
                .tagDescriptions(TagDescription.builder()
                        .tags(Tag.builder()
                                .key(EXISTING_TAG_KEY)
                                .value(EXISTING_TAG_VALUE)
                                .build())
                        .resourceArn(RESOURCE_REFERENCE)
                        .build())
                .build());
        when(awsTaggingService.prepareElasticLoadBalancingTags(USER_DEFINED_TAGS)).thenReturn(expectedTags);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(elbClient).addTags(AddTagsRequest.builder()
                .resourceArns(RESOURCE_REFERENCE)
                .tags(expectedTags)
                .build());
    }

    private List<Tag> toElbTags(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .key(e.getKey())
                        .value(e.getValue())
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