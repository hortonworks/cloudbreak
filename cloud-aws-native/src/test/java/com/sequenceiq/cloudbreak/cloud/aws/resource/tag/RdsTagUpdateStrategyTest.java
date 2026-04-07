package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonRdsClient;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.rds.model.AddTagsToResourceRequest;
import software.amazon.awssdk.services.rds.model.Tag;

@ExtendWith(MockitoExtension.class)
class RdsTagUpdateStrategyTest {

    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AmazonRdsClient rdsClient;

    @InjectMocks
    private RdsTagUpdateStrategy underTest;

    @Test
    void testUpdateTagsForRdsInstance() {
        CloudResource cloudResource = buildResource(ResourceType.RDS_INSTANCE, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createRdsClient(authenticatedContext)).thenReturn(rdsClient);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(rdsClient).addTagsToResource(AddTagsToResourceRequest.builder()
                .resourceName(RESOURCE_REFERENCE)
                .tags(toRdsTags(USER_DEFINED_TAGS))
                .build());
    }

    @Test
    void testUpdateTagsForRdsDbSubnetGroup() {
        CloudResource cloudResource = buildResource(ResourceType.RDS_DB_SUBNET_GROUP, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createRdsClient(authenticatedContext)).thenReturn(rdsClient);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(rdsClient).addTagsToResource(any(AddTagsToResourceRequest.class));
    }

    @Test
    void testUpdateTagsForRdsDbParameterGroup() {
        CloudResource cloudResource = buildResource(ResourceType.RDS_DB_PARAMETER_GROUP, null, RESOURCE_REFERENCE);
        when(commonAwsClient.createRdsClient(authenticatedContext)).thenReturn(rdsClient);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(rdsClient).addTagsToResource(any(AddTagsToResourceRequest.class));
    }

    private List<Tag> toRdsTags(Map<String, String> tags) {
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