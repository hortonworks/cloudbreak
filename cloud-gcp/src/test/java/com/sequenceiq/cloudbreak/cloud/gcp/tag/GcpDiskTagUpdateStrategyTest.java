package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.ZoneSetLabelsRequest;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpDiskTagUpdateStrategyTest {

    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    private static final String REGION = "us-central1";

    private static final String RESOURCE_NAME = "test-resource";

    private static final String FINGERPRINT = "abc123fingerprint";

    private static final Map<String, String> EXISTING_LABELS = Map.of("existingKey", "existingValue");

    private static final Map<String, String> NEW_LABELS = Map.of("newKey", "newValue");

    private static final Map<String, String> MERGED_LABELS = Map.of("existingKey", "existingValue", "newKey", "newValue");

    @Mock
    private GcpContext gcpContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private Compute compute;

    @Mock
    private Compute.Disks disks;

    @Mock
    private Compute.Disks.Get disksGet;

    @Mock
    private Compute.Disks.SetLabels disksSetLabels;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private GcpContextBuilder gcpContextBuilder;

    @InjectMocks
    private GcpDiskTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(gcpContextBuilder.contextInit(cloudContext, authenticatedContext, null, true)).thenReturn(gcpContext);
    }

    private static Stream<Arguments> diskResourceTypes() {
        return Stream.of(
                Arguments.of(ResourceType.GCP_DISK),
                Arguments.of(ResourceType.GCP_ATTACHED_DISK)
        );
    }

    @ParameterizedTest
    @MethodSource("diskResourceTypes")
    void testUpdateTags(ResourceType resourceType) throws Exception {
        CloudResource cloudResource = buildCloudResource(resourceType);
        Disk disk = new Disk()
                .setLabelFingerprint(FINGERPRINT)
                .setLabels(EXISTING_LABELS);

        when(compute.disks()).thenReturn(disks);
        when(disks.get(PROJECT_ID, ZONE, RESOURCE_NAME)).thenReturn(disksGet);
        when(disksGet.execute()).thenReturn(disk);
        when(disks.setLabels(eq(PROJECT_ID), eq(ZONE), eq(RESOURCE_NAME), any(ZoneSetLabelsRequest.class)))
                .thenReturn(disksSetLabels);

        underTest.updateTags(authenticatedContext, cloudResource, NEW_LABELS);

        verify(disks).setLabels(eq(PROJECT_ID), eq(ZONE), eq(RESOURCE_NAME),
                argThat(req -> FINGERPRINT.equals(req.getLabelFingerprint())
                        && MERGED_LABELS.equals(req.getLabels())));
        verify(disksSetLabels).execute();
    }

    @Test
    void testUpdateTagsWithoutNewTags() throws Exception {
        CloudResource cloudResource = buildCloudResource(ResourceType.GCP_DISK);
        Disk disk = new Disk()
                .setLabelFingerprint(FINGERPRINT)
                .setLabels(EXISTING_LABELS);

        when(compute.disks()).thenReturn(disks);
        when(disks.get(PROJECT_ID, ZONE, RESOURCE_NAME)).thenReturn(disksGet);
        when(disksGet.execute()).thenReturn(disk);

        underTest.updateTags(authenticatedContext, cloudResource, EXISTING_LABELS);

        verify(disksSetLabels, times(0)).execute();
    }

    private CloudResource buildCloudResource(ResourceType type) {
        return CloudResource.builder()
                .withType(type)
                .withName(RESOURCE_NAME)
                .withAvailabilityZone(ZONE)
                .build();
    }
}