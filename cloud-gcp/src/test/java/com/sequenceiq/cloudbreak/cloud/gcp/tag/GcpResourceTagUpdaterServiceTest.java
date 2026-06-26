package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISK;
import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISKSET;
import static com.sequenceiq.common.api.type.ResourceType.GCP_DISK;
import static com.sequenceiq.common.api.type.ResourceType.GCP_INSTANCE;
import static com.sequenceiq.common.api.type.ResourceType.GCP_SUBNET;
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

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpResourceTagUpdaterServiceTest {

    private static final String INSTANCE_ID = "instanceId";

    private static final String RESOURCE_REFERENCE = "resourceReference";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private GcpInstanceTagUpdateStrategy instanceStrategy;

    @Mock
    private GcpDiskTagUpdateStrategy diskStrategy;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    private GcpResourceTagUpdaterService underTest;

    @BeforeEach
    void setUp() {
        when(instanceStrategy.supportedTypes()).thenReturn(Set.of(GCP_INSTANCE));
        when(diskStrategy.supportedTypes()).thenReturn(Set.of(GCP_DISK, GCP_ATTACHED_DISK, GCP_ATTACHED_DISKSET));
        underTest = new GcpResourceTagUpdaterService(List.of(instanceStrategy, diskStrategy), gcpLabelUtil);
    }

    @Test
    void testUpdateTagsGcpInstance() throws IOException {
        CloudResource cloudResource = buildResource(GCP_INSTANCE, INSTANCE_ID, null);
        when(instanceStrategy.isBatchUpdateSupported()).thenReturn(false);
        when(gcpLabelUtil.createLabelsFromTagsMap(USER_DEFINED_TAGS)).thenReturn(USER_DEFINED_TAGS);

        underTest.updateTags(authenticatedContext, List.of(cloudResource), USER_DEFINED_TAGS);

        verify(instanceStrategy).updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);
        verifyNoMoreInteractions(diskStrategy);
    }

    @Test
    void testUpdateTagsGcpDisk() throws IOException {
        CloudResource cloudResource = buildResource(GCP_DISK, null, RESOURCE_REFERENCE);
        when(diskStrategy.isBatchUpdateSupported()).thenReturn(false);
        when(gcpLabelUtil.createLabelsFromTagsMap(USER_DEFINED_TAGS)).thenReturn(USER_DEFINED_TAGS);

        underTest.updateTags(authenticatedContext, List.of(cloudResource), USER_DEFINED_TAGS);

        verify(diskStrategy).updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);
        verifyNoMoreInteractions(instanceStrategy);
    }

    @Test
    void testUpdateTagsUnsupportedType() throws IOException {
        CloudResource cloudResource = buildResource(GCP_SUBNET, null, RESOURCE_REFERENCE);

        underTest.updateTags(authenticatedContext, List.of(cloudResource), USER_DEFINED_TAGS);

        verifyNoMoreInteractions(instanceStrategy, diskStrategy);
    }

    @Test
    void testUpdateTagsWhenRuntimeExceptionOccurs() throws IOException {
        CloudResource cloudResource = buildResource(GCP_INSTANCE, INSTANCE_ID, null);
        doThrow(new RuntimeException("GCP error")).when(instanceStrategy)
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