package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.tag;

import static com.sequenceiq.common.api.type.ResourceType.AWS_EFS;
import static com.sequenceiq.common.api.type.ResourceType.AWS_LAUNCHCONFIGURATION;
import static com.sequenceiq.common.api.type.ResourceType.AWS_ROOT_DISK;
import static com.sequenceiq.common.api.type.ResourceType.AWS_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.CLOUDFORMATION_STACK;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
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
class AwsResourceTagUpdaterServiceTest {

    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private TagUpdateStrategy ec2Strategy;

    @Mock
    private TagUpdateStrategy cloudFormationStrategy;

    private AwsResourceTagUpdaterService underTest;

    @BeforeEach
    void setUp() {
        when(ec2Strategy.supportedTypes()).thenReturn(Set.of(AWS_ROOT_DISK, AWS_VOLUMESET));
        when(cloudFormationStrategy.supportedTypes()).thenReturn(Set.of(CLOUDFORMATION_STACK));
        underTest = new AwsResourceTagUpdaterService(List.of(ec2Strategy, cloudFormationStrategy));
    }

    @Test
    void testUpdateTagsAwsRootDisk() throws IOException {
        CloudResource cloudResource = buildResource(AWS_ROOT_DISK, INSTANCE_ID, null);

        underTest.updateTags(authenticatedContext, List.of(cloudResource), USER_DEFINED_TAGS);

        verify(ec2Strategy).updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);
        verifyNoMoreInteractions(cloudFormationStrategy);
    }

    @Test
    void testUpdateTagsCloudFormationStack() throws IOException {
        CloudResource cloudResource = buildResource(CLOUDFORMATION_STACK, null, RESOURCE_REFERENCE);

        underTest.updateTags(authenticatedContext, List.of(cloudResource), USER_DEFINED_TAGS);

        verify(cloudFormationStrategy).updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);
        verifyNoMoreInteractions(ec2Strategy);
    }

    @Test
    void testUpdateTagsUnsupportedType() {
        CloudResource cloudResource = buildResource(AWS_EFS, null, RESOURCE_REFERENCE);

        underTest.updateTags(authenticatedContext, List.of(cloudResource), USER_DEFINED_TAGS);

        verifyNoMoreInteractions(ec2Strategy, cloudFormationStrategy);
    }

    @Test
    void testUpdateTagsNotTaggableResourceType() {
        CloudResource cloudResource = buildResource(AWS_LAUNCHCONFIGURATION, null, RESOURCE_REFERENCE);

        underTest.updateTags(authenticatedContext, List.of(cloudResource), USER_DEFINED_TAGS);

        verifyNoMoreInteractions(ec2Strategy, cloudFormationStrategy);
    }

    @Test
    void testUpdateTagsWhenRuntimeExceptionOccurs() throws IOException {
        CloudResource cloudResource = buildResource(AWS_ROOT_DISK, INSTANCE_ID, null);
        doThrow(new RuntimeException("AWS error")).when(ec2Strategy)
                .updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        assertThrows(RuntimeException.class, () -> underTest.updateTags(authenticatedContext, List.of(cloudResource), USER_DEFINED_TAGS));
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