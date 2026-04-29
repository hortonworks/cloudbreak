package com.sequenceiq.cloudbreak.cloud.azure.tag;

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
class AzureResourceTagUpdaterServiceTest {
    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private TagUpdateStrategy instanceStrategy;

    @Mock
    private TagUpdateStrategy diskStrategy;

    private AzureResourceTagUpdaterService underTest;

    @BeforeEach
    void setUp() {
        when(instanceStrategy.supportedTypes()).thenReturn(Set.of(ResourceType.AZURE_INSTANCE));
        when(diskStrategy.supportedTypes()).thenReturn(Set.of(ResourceType.AZURE_DISK));
        underTest = new AzureResourceTagUpdaterService(List.of(instanceStrategy, diskStrategy));
    }

    @Test
    void testUpdateTagsAzureInstance() throws IOException {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_INSTANCE, INSTANCE_ID, null);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(instanceStrategy).updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);
        verifyNoMoreInteractions(diskStrategy);
    }

    @Test
    void testUpdateTagsAzureDisk() throws IOException {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_DISK, null, RESOURCE_REFERENCE);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(diskStrategy).updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);
        verifyNoMoreInteractions(instanceStrategy);
    }

    @Test
    void testUpdateTagsUnsupportedType() throws IOException {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_SUBNET, null, RESOURCE_REFERENCE);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verifyNoMoreInteractions(instanceStrategy, diskStrategy);
    }

    @Test
    void testUpdateTagsNotTaggableResourceType() throws IOException {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_DATABASE_SECURITY_ALERT_POLICY, null, RESOURCE_REFERENCE);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verifyNoMoreInteractions(instanceStrategy, diskStrategy);
    }

    @Test
    void testUpdateTagsWhenRuntimeExceptionOccurs() throws IOException {
        CloudResource cloudResource = buildResource(ResourceType.AZURE_INSTANCE, INSTANCE_ID, null);
        doThrow(new RuntimeException("Azure error")).when(instanceStrategy)
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