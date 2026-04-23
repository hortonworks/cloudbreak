package com.sequenceiq.cloudbreak.cloud.gcp.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpAttachedDiskSetTagUpdateStrategyTest {
    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    private static final String RESOURCE_NAME = "test-resource";

    private static final String DISK_ID_1 = "disk-name-1";

    private static final String DISK_ID_2 = "disk-name-2";

    private static final String FINGERPRINT_1 = "fingerprint1";

    private static final String FINGERPRINT_2 = "fingerprint2";

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
    private Compute.Disks.Get disksGet1;

    @Mock
    private Compute.Disks.Get disksGet2;

    @Mock
    private Compute.Disks.SetLabels disksSetLabels1;

    @Mock
    private Compute.Disks.SetLabels disksSetLabels2;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private GcpContextBuilder gcpContextBuilder;

    @InjectMocks
    private GcpAttachedDiskSetTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(gcpContextBuilder.contextInit(cloudContext, authenticatedContext, null, true)).thenReturn(gcpContext);
    }

    @Test
    void testUpdateTags() throws Exception {
        CloudResource cloudResource = buildCloudResourceWithVolumes(
                List.of(
                        buildVolume(DISK_ID_1),
                        buildVolume(DISK_ID_2)
                )
        );

        Disk disk1 = new Disk().setLabelFingerprint(FINGERPRINT_1).setLabels(EXISTING_LABELS);
        Disk disk2 = new Disk().setLabelFingerprint(FINGERPRINT_2).setLabels(EXISTING_LABELS);

        when(compute.disks()).thenReturn(disks);
        when(disks.get(PROJECT_ID, ZONE, DISK_ID_1)).thenReturn(disksGet1);
        when(disks.get(PROJECT_ID, ZONE, DISK_ID_2)).thenReturn(disksGet2);
        when(disksGet1.execute()).thenReturn(disk1);
        when(disksGet2.execute()).thenReturn(disk2);
        when(disks.setLabels(eq(PROJECT_ID), eq(ZONE), eq(DISK_ID_1), any(ZoneSetLabelsRequest.class)))
                .thenReturn(disksSetLabels1);
        when(disks.setLabels(eq(PROJECT_ID), eq(ZONE), eq(DISK_ID_2), any(ZoneSetLabelsRequest.class)))
                .thenReturn(disksSetLabels2);

        underTest.updateTags(authenticatedContext, cloudResource, NEW_LABELS);
        verify(disks).setLabels(eq(PROJECT_ID), eq(ZONE), eq(DISK_ID_1),
                argThat(req -> FINGERPRINT_1.equals(req.getLabelFingerprint())
                        && MERGED_LABELS.equals(req.getLabels())));
        verify(disks).setLabels(eq(PROJECT_ID), eq(ZONE), eq(DISK_ID_2),
                argThat(req -> FINGERPRINT_2.equals(req.getLabelFingerprint())
                        && MERGED_LABELS.equals(req.getLabels())));
        verify(disksSetLabels1).execute();
        verify(disksSetLabels2).execute();
    }

    @Test
    void testUpdateTagsWithoutNewTags() throws Exception {
        CloudResource cloudResource = buildCloudResourceWithVolumes(
                List.of(
                        buildVolume(DISK_ID_1)
                )
        );

        Disk disk1 = new Disk().setLabelFingerprint(FINGERPRINT_1).setLabels(EXISTING_LABELS);

        when(compute.disks()).thenReturn(disks);
        when(disks.get(PROJECT_ID, ZONE, DISK_ID_1)).thenReturn(disksGet1);
        when(disksGet1.execute()).thenReturn(disk1);

        underTest.updateTags(authenticatedContext, cloudResource, EXISTING_LABELS);

        verify(disksSetLabels1, times(0)).execute();
    }

    private CloudResource buildCloudResourceWithVolumes(List<VolumeSetAttributes.Volume> volumes) {
        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder()
                .withAvailabilityZone(ZONE)
                .withVolumes(volumes)
                .build();

        return CloudResource.builder()
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withName(RESOURCE_NAME)
                .withAvailabilityZone(ZONE)
                .withParameters(Map.of(CloudResource.ATTRIBUTES, attributes))
                .build();
    }

    private VolumeSetAttributes.Volume buildVolume(String diskId) {
        return new VolumeSetAttributes.Volume(diskId, "device", 100, "type", null);
    }
}