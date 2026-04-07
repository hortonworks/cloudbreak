package com.sequenceiq.cloudbreak.cloud.aws.resource.tag;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.TagUpdateStrategy;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AwsNativeResourceTagUpdaterServiceTest {

    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private TagUpdateStrategy ec2Strategy;

    @Mock
    private TagUpdateStrategy elbStrategy;

    private AwsNativeResourceTagUpdaterService underTest;

    @BeforeEach
    void setUp() {
        when(ec2Strategy.supportedTypes()).thenReturn(Set.of(ResourceType.AWS_INSTANCE));
        when(elbStrategy.supportedTypes()).thenReturn(Set.of(ResourceType.ELASTIC_LOAD_BALANCER));
        underTest = new AwsNativeResourceTagUpdaterService(List.of(ec2Strategy, elbStrategy));
    }

    @Test
    void testUpdateTagsAwsInstance() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_INSTANCE, INSTANCE_ID, null);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Strategy).updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);
        verifyNoMoreInteractions(elbStrategy);
    }

    @Test
    void testUpdateTagsAwsLoadBalancer() {
        CloudResource cloudResource = buildResource(ResourceType.ELASTIC_LOAD_BALANCER, null, RESOURCE_REFERENCE);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(elbStrategy).updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);
        verifyNoMoreInteractions(ec2Strategy);
    }

    @Test
    void testUpdateTagsUnsupportedType() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_EFS, null, RESOURCE_REFERENCE);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verifyNoMoreInteractions(ec2Strategy, elbStrategy);
    }

    @Test
    void testUpdateTagsWhenRuntimeExceptionOccurs() {
        CloudResource cloudResource = buildResource(ResourceType.AWS_INSTANCE, INSTANCE_ID, null);
        doThrow(new RuntimeException("AWS error")).when(ec2Strategy)
                .updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        assertThrows(RuntimeException.class, () -> underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS));
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