package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskType.LOCAL_SSD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Snapshot;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.DiskTypeChangeResumePoint;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpCreateDiskParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskAttachmentParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskCreationSpec;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskPlan;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskUpdateRetryService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpInstanceRetrievalService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResizeDiskParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpReusedDisk;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpSnapshotParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.service.ResumeAction;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeUpdateResult;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.VolumeInfo;

@ExtendWith(MockitoExtension.class)
class GcpResourceVolumeConnectorTest {

    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    private static final String DEVICE_NAME_PREFIX = "/dev/disk/by-id/google-";

    @InjectMocks
    private GcpResourceVolumeConnector underTest;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpInstanceRetrievalService gcpInstanceRetrievalService;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private Compute compute;

    @Mock
    private GcpContextBuilder gcpContextBuilder;

    @Mock
    private GcpDiskUpdateService gcpDiskUpdateService;

    @Mock
    private GcpDiskUpdateRetryService gcpDiskUpdateRetryService;

    @Mock
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private GcpContext gcpContext;

    private void mockContext() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(gcpContextBuilder.contextInit(cloudContext, authenticatedContext, null, true)).thenReturn(gcpContext);
        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
    }

    private void mockExecutorRunsInline() {
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<?> callable = invocation.getArgument(0);
            CompletableFuture<Object> future = new CompletableFuture<>();
            try {
                future.complete(callable.call());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            return future;
        });
    }

    private void mockContextAndExecutor() {
        mockContext();
        mockExecutorRunsInline();
    }

    @Test
    void testUpdateDiskVolumesResizesUndersizedDisk() throws Exception {
        mockContextAndExecutor();

        underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200, null, List.of(createZonedDiskResource("i1v1", 100)));

        ArgumentCaptor<GcpResizeDiskParameters> captor = ArgumentCaptor.forClass(GcpResizeDiskParameters.class);
        verify(gcpDiskUpdateRetryService).resizeDisk(captor.capture(), eq(gcpContext));
        GcpResizeDiskParameters params = captor.getValue();
        assertEquals(PROJECT_ID, params.projectId());
        assertEquals(ZONE, params.preferredZone());
        assertEquals("i1v1", params.diskName());
        assertEquals(200, params.size());
        assertEquals(compute, params.compute());
    }

    @Test
    void testUpdateDiskVolumesAggregatesFailures() throws Exception {
        mockContextAndExecutor();
        doThrow(new IOException("boom")).when(gcpDiskUpdateRetryService).resizeDisk(any(GcpResizeDiskParameters.class), eq(gcpContext));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200, null,
                        List.of(createZonedDiskResource("i1v1", 100))));
        assertTrue(exception.getMessage().contains("i1v1"));
        assertTrue(exception.getMessage().contains("boom"), "The aggregated exception should surface the underlying GCP failure cause");
    }

    @Test
    void testUpdateDiskVolumesResolvesZoneFromVolumeSetAttributes() throws Exception {
        mockContextAndExecutor();

        underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200, null,
                List.of(createDiskResourceWithAttributeZone("i1v1", 100)));

        ArgumentCaptor<GcpResizeDiskParameters> captor = ArgumentCaptor.forClass(GcpResizeDiskParameters.class);
        verify(gcpDiskUpdateRetryService).resizeDisk(captor.capture(), eq(gcpContext));
        assertEquals(ZONE, captor.getValue().preferredZone());
    }

    @Test
    void testUpdateDiskVolumesFailsWhenZoneCannotBeResolved() throws Exception {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(gcpContextBuilder.contextInit(cloudContext, authenticatedContext, null, true)).thenReturn(gcpContext);

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200, null,
                        List.of(createDiskResourceWithoutZone("i1v1", 100))));
        assertTrue(exception.getMessage().contains("i1v1"));
        verify(gcpDiskUpdateRetryService, never()).resizeDisk(any(GcpResizeDiskParameters.class), any(GcpContext.class));
    }

    @Test
    void testUpdateDiskVolumesIgnoresNonDiskSetResources() throws Exception {
        mockContextAndExecutor();
        CloudResource nonDiskSetResource = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withName("instance-resource")
                .withAvailabilityZone(ZONE)
                .withParameters(new HashMap<>())
                .build();

        underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200, null,
                List.of(nonDiskSetResource, createZonedDiskResource("i1v1", 100)));

        ArgumentCaptor<GcpResizeDiskParameters> captor = ArgumentCaptor.forClass(GcpResizeDiskParameters.class);
        verify(gcpDiskUpdateRetryService).resizeDisk(captor.capture(), eq(gcpContext));
        assertEquals("i1v1", captor.getValue().diskName());
    }

    @Test
    void changeDiskTypeMigratesDiskThroughSnapshotAndSwapInOrder() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        Map<String, VolumeUpdateResult> result =
                underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource));

        InOrder inOrder = inOrder(gcpDiskUpdateRetryService);
        inOrder.verify(gcpDiskUpdateRetryService).createSnapshot(any(GcpSnapshotParameters.class), any(Snapshot.class), eq(gcpContext));
        inOrder.verify(gcpDiskUpdateRetryService).insertDisk(any(GcpCreateDiskParameters.class));
        inOrder.verify(gcpDiskUpdateRetryService).pollDiskOperations(eq(authenticatedContext), eq(gcpContext), anyList());
        inOrder.verify(gcpDiskUpdateRetryService).detachDiskFromInstance(any(GcpDiskAttachmentParameters.class), eq(gcpContext));
        inOrder.verify(gcpDiskUpdateRetryService).attachDiskToInstance(any(GcpDiskAttachmentParameters.class), any(AttachedDisk.class), eq(gcpContext));
        inOrder.verify(gcpDiskUpdateRetryService).deleteDisk(any(GcpDiskAttachmentParameters.class), eq(gcpContext));
        inOrder.verify(gcpDiskUpdateRetryService).deleteSnapshot(any(GcpSnapshotParameters.class), eq(gcpContext));

        // the new disk carries the deterministic new name, target type url and the deterministic (timestamp-free) source snapshot
        ArgumentCaptor<GcpCreateDiskParameters> createCaptor = ArgumentCaptor.forClass(GcpCreateDiskParameters.class);
        verify(gcpDiskUpdateRetryService).insertDisk(createCaptor.capture());
        Disk newDisk = createCaptor.getValue().disk();
        assertEquals("i1v1-pdssd", newDisk.getName());
        assertEquals(100L, newDisk.getSizeGb());
        assertTrue(newDisk.getType().endsWith("pd-ssd"), "The new disk must be created with the target type url");
        assertEquals("https://www.googleapis.com/compute/v1/projects/test-project/global/snapshots/i1v1-typechange",
                newDisk.getSourceSnapshot());

        // the rename is reported back keyed by the original volume id, instead of mutating the shared volume attributes
        VolumeUpdateResult updateResult = result.get("i1v1");
        assertEquals("i1v1-pdssd", updateResult.newVolumeId());
        assertEquals(DEVICE_NAME_PREFIX + "i1v1-pdssd", updateResult.newDevice());
        assertEquals(100, updateResult.newSize());
        // the source volume attributes are left untouched by the connector
        assertEquals("i1v1", volume.getId());
    }

    @Test
    void changeDiskTypeFailsImmediatelyWhenSnapshotCreationFailsWithNothingToClean() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        when(gcpDiskUpdateRetryService.createSnapshot(any(GcpSnapshotParameters.class), any(Snapshot.class), eq(gcpContext)))
                .thenThrow(new CloudbreakServiceException("snapshot boom"));
        CloudResource resource = createTypeChangeResource("instance1", "master",
                new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard", CloudVolumeUsageType.GENERAL));
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource)));

        assertTrue(exception.getMessage().contains("i1v1"));
        verify(gcpDiskUpdateRetryService, never()).insertDisk(any(GcpCreateDiskParameters.class));
        verify(gcpDiskUpdateRetryService, never()).deleteSnapshot(any(GcpSnapshotParameters.class), any(GcpContext.class));
        verify(gcpDiskUpdateRetryService, never()).detachDiskFromInstance(any(GcpDiskAttachmentParameters.class), any(GcpContext.class));
    }

    @Test
    void changeDiskTypeDeletesSnapshotWhenNewDiskCreationFails() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        when(gcpDiskUpdateRetryService.insertDisk(any(GcpCreateDiskParameters.class))).thenThrow(new CloudbreakServiceException("insert boom"));
        CloudResource resource = createTypeChangeResource("instance1", "master",
                new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard", CloudVolumeUsageType.GENERAL));
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource)));

        assertTrue(exception.getMessage().contains("i1v1"));
        verify(gcpDiskUpdateRetryService).deleteSnapshot(any(GcpSnapshotParameters.class), eq(gcpContext));
        verify(gcpDiskUpdateRetryService, never()).detachDiskFromInstance(any(GcpDiskAttachmentParameters.class), any(GcpContext.class));
    }

    @Test
    void changeDiskTypeDeletesNewDiskAndSnapshotWhenDetachFails() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        when(gcpDiskUpdateRetryService.detachDiskFromInstance(any(GcpDiskAttachmentParameters.class), eq(gcpContext)))
                .thenThrow(new CloudbreakServiceException("detach boom"));
        CloudResource resource = createTypeChangeResource("instance1", "master",
                new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard", CloudVolumeUsageType.GENERAL));
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource)));

        assertTrue(exception.getMessage().contains("i1v1"));
        ArgumentCaptor<GcpDiskAttachmentParameters> deleteCaptor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        verify(gcpDiskUpdateRetryService).deleteDisk(deleteCaptor.capture(), eq(gcpContext));
        assertEquals("i1v1-pdssd", deleteCaptor.getValue().diskName());
        verify(gcpDiskUpdateRetryService).deleteSnapshot(any(GcpSnapshotParameters.class), eq(gcpContext));
        verify(gcpDiskUpdateRetryService, never()).attachDiskToInstance(any(GcpDiskAttachmentParameters.class), any(AttachedDisk.class), any(GcpContext.class));
    }

    @Test
    void changeDiskTypeReattachesOldDiskAndCleansUpWhenAttachFails() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        when(gcpDiskUpdateRetryService.attachDiskToInstance(any(GcpDiskAttachmentParameters.class), any(AttachedDisk.class), eq(gcpContext)))
                .thenThrow(new CloudbreakServiceException("attach boom"));
        CloudResource resource = createTypeChangeResource("instance1", "master",
                new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard", CloudVolumeUsageType.GENERAL));
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource)));

        assertTrue(exception.getMessage().contains("i1v1"));
        // the failed new-disk attach is followed by a best-effort re-attach of the old disk (2 attach calls total)
        ArgumentCaptor<GcpDiskAttachmentParameters> attachCaptor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        verify(gcpDiskUpdateRetryService, times(2)).attachDiskToInstance(attachCaptor.capture(), any(AttachedDisk.class), eq(gcpContext));
        assertEquals(List.of("i1v1-pdssd", "i1v1"), attachCaptor.getAllValues().stream().map(GcpDiskAttachmentParameters::diskName).toList());
        // the new disk and the snapshot are cleaned up
        verify(gcpDiskUpdateRetryService).deleteDisk(any(GcpDiskAttachmentParameters.class), eq(gcpContext));
        verify(gcpDiskUpdateRetryService).deleteSnapshot(any(GcpSnapshotParameters.class), eq(gcpContext));
    }

    @Test
    void changeDiskTypeSucceedsWhenOldDiskDeletionFails() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        when(gcpDiskUpdateRetryService.deleteDisk(any(GcpDiskAttachmentParameters.class), eq(gcpContext)))
                .thenThrow(new CloudbreakServiceException("delete old boom"));
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        Map<String, VolumeUpdateResult> result =
                underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource));

        // deleting the old disk is not fatal: the snapshot is still cleaned up and the rename is still reported back
        verify(gcpDiskUpdateRetryService).deleteSnapshot(any(GcpSnapshotParameters.class), eq(gcpContext));
        assertEquals("i1v1-pdssd", result.get("i1v1").newVolumeId());
    }

    @Test
    void changeDiskTypeSucceedsWhenSnapshotDeletionFails() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        when(gcpDiskUpdateRetryService.deleteSnapshot(any(GcpSnapshotParameters.class), eq(gcpContext)))
                .thenThrow(new CloudbreakServiceException("delete snapshot boom"));
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        Map<String, VolumeUpdateResult> result =
                underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource));

        // deleting the snapshot is not fatal: the type change still succeeds and the rename is still reported back
        assertEquals("i1v1-pdssd", result.get("i1v1").newVolumeId());
    }

    @Test
    void changeDiskTypeSkipsLocalSsdVolumes() throws Exception {
        mockContext();
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/sdc", 375, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource));

        verify(gcpDiskUpdateRetryService, never()).createSnapshot(any(GcpSnapshotParameters.class), any(Snapshot.class), any(GcpContext.class));
        // a local ssd is never migrated, so its attributes are untouched
        assertEquals("i1v1", volume.getId());
        assertEquals(LOCAL_SSD.value(), volume.getType());
    }

    @Test
    void changeDiskTypeAggregatesFailuresAndMigratesTheHealthyDisk() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        when(gcpDiskUpdateRetryService.createSnapshot(any(GcpSnapshotParameters.class), any(Snapshot.class), eq(gcpContext)))
                .thenAnswer(invocation -> {
                    GcpSnapshotParameters params = invocation.getArgument(0);
                    if ("i1v1".equals(params.sourceDiskName())) {
                        throw new CloudbreakServiceException("snapshot boom");
                    }
                    return List.of();
                });
        VolumeSetAttributes.Volume failing = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume healthy = new VolumeSetAttributes.Volume("i1v2", "/dev/disk/by-id/google-i1v2", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", failing, healthy);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1", "i1v2"), "pd-ssd", 0, cloudStack, List.of(resource)));

        assertTrue(exception.getMessage().contains("i1v1"), "The aggregated exception should name the failed disk");
        // on partial failure the connector throws and reports nothing back, so the source volume attributes stay untouched;
        // the healthy disk was still migrated at the provider (a new disk was created for it)
        assertEquals("i1v1", failing.getId());
        assertEquals("i1v2", healthy.getId());
        ArgumentCaptor<GcpCreateDiskParameters> createCaptor = ArgumentCaptor.forClass(GcpCreateDiskParameters.class);
        verify(gcpDiskUpdateRetryService).insertDisk(createCaptor.capture());
        assertEquals("i1v2-pdssd", createCaptor.getValue().disk().getName());
    }

    @Test
    void changeDiskTypeAndSizeCreatesNewDiskAtRequestedSize() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        // a combined type + size change: type pd-ssd and a size increase to 200 GB
        Map<String, VolumeUpdateResult> result =
                underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 200, cloudStack, List.of(resource));

        ArgumentCaptor<GcpCreateDiskParameters> createCaptor = ArgumentCaptor.forClass(GcpCreateDiskParameters.class);
        verify(gcpDiskUpdateRetryService).insertDisk(createCaptor.capture());
        assertEquals(200L, createCaptor.getValue().disk().getSizeGb(), "The new disk must be created at the requested size, not the old size");
        assertEquals(200, result.get("i1v1").newSize(), "The reported size must reflect the requested size after the swap");
    }

    @Test
    void changeDiskTypeFloorsNewDiskSizeAtCurrentSizeWhenRequestedSmaller() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        // a smaller requested size cannot shrink a snapshot-restored disk, so it is floored at the current size
        Map<String, VolumeUpdateResult> result =
                underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 50, cloudStack, List.of(resource));

        ArgumentCaptor<GcpCreateDiskParameters> createCaptor = ArgumentCaptor.forClass(GcpCreateDiskParameters.class);
        verify(gcpDiskUpdateRetryService).insertDisk(createCaptor.capture());
        assertEquals(100L, createCaptor.getValue().disk().getSizeGb(), "The new disk must not be created smaller than the source disk");
        assertEquals(100, result.get("i1v1").newSize());
    }

    @Test
    void changeDiskTypeWarnsAndDoesNotThrowWhenNoVolumeMatches() throws Exception {
        mockContext();
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        // a requested id that matches no attached disk migrates nothing, but the update still succeeds (warn-only)
        underTest.updateDiskVolumes(authenticatedContext, List.of("i1v9"), "pd-ssd", 0, cloudStack, List.of(resource));

        verify(gcpDiskUpdateRetryService, never()).createSnapshot(any(GcpSnapshotParameters.class), any(Snapshot.class), any(GcpContext.class));
        assertEquals("i1v1", volume.getId());
        assertEquals("pd-standard", volume.getType());
    }

    @Test
    void changeDiskTypeGivesTruncatedNewDiskNamesDistinctHashesToAvoidCollision() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        // two disk ids longer than the 63-char GCP limit that share the same truncated prefix; without a hash the derived
        // new-disk names would collide and the wrong disk could be attached (525908)
        String sharedPrefix = "a".repeat(55);
        String longId1 = sharedPrefix + "1111111";
        String longId2 = sharedPrefix + "2222222";
        VolumeSetAttributes.Volume volume1 = new VolumeSetAttributes.Volume(longId1, DEVICE_NAME_PREFIX + longId1, 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume volume2 = new VolumeSetAttributes.Volume(longId2, DEVICE_NAME_PREFIX + longId2, 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume1, volume2);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        underTest.updateDiskVolumes(authenticatedContext, List.of(longId1, longId2), "pd-ssd", 0, cloudStack, List.of(resource));

        ArgumentCaptor<GcpCreateDiskParameters> createCaptor = ArgumentCaptor.forClass(GcpCreateDiskParameters.class);
        verify(gcpDiskUpdateRetryService, times(2)).insertDisk(createCaptor.capture());
        List<String> newNames = createCaptor.getAllValues().stream().map(params -> params.disk().getName()).toList();
        assertEquals(2, newNames.stream().distinct().count(), "Two long source names sharing a truncated prefix must map to distinct new disk names");
        newNames.forEach(name -> assertTrue(name.length() <= 63, "The new disk name must fit the GCP 63-character limit, but was: " + name));
    }

    @Test
    void changeDiskTypeUsesDeterministicTimestampFreeSnapshotName() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource));

        // the snapshot name is stable (no epoch-millis suffix) so a rerun can adopt/clean up the same snapshot (525909)
        ArgumentCaptor<Snapshot> snapshotCaptor = ArgumentCaptor.forClass(Snapshot.class);
        verify(gcpDiskUpdateRetryService).createSnapshot(any(GcpSnapshotParameters.class), snapshotCaptor.capture(), eq(gcpContext));
        assertEquals("i1v1-typechange", snapshotCaptor.getValue().getName());
    }

    @Test
    void changeDiskTypeCleansUpOnlyWhenResumePointIsCleanupOnly() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        // a prior attempt already completed the swap: the deterministically-named new disk exists and is attached to the
        // target instance, so this rerun must not re-snapshot the (already deleted) source disk (525907)
        when(gcpDiskUpdateRetryService.resolveDiskTypeChangeResumePoint(any(GcpDiskAttachmentParameters.class),
                any(GcpDiskAttachmentParameters.class), anyString()))
                .thenReturn(new DiskTypeChangeResumePoint(ResumeAction.CLEANUP_ONLY, Optional.of(new Disk().setSizeGb(150L))));
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        Map<String, VolumeUpdateResult> result =
                underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource));

        // nothing is re-created or swapped; the leftover old disk and snapshot are cleaned up best-effort
        verify(gcpDiskUpdateRetryService, never()).createSnapshot(any(GcpSnapshotParameters.class), any(Snapshot.class), any(GcpContext.class));
        verify(gcpDiskUpdateRetryService, never()).insertDisk(any(GcpCreateDiskParameters.class));
        verify(gcpDiskUpdateRetryService, never()).detachDiskFromInstance(any(GcpDiskAttachmentParameters.class), any(GcpContext.class));
        verify(gcpDiskUpdateRetryService, never()).attachDiskToInstance(any(GcpDiskAttachmentParameters.class), any(AttachedDisk.class), any(GcpContext.class));
        ArgumentCaptor<GcpDiskAttachmentParameters> deleteCaptor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        verify(gcpDiskUpdateRetryService).deleteDisk(deleteCaptor.capture(), eq(gcpContext));
        assertEquals("i1v1", deleteCaptor.getValue().diskName(), "The leftover old disk must be the one cleaned up");
        verify(gcpDiskUpdateRetryService).deleteSnapshot(any(GcpSnapshotParameters.class), eq(gcpContext));
        // the rename is reported using the existing new disk's provider-authoritative size
        VolumeUpdateResult updateResult = result.get("i1v1");
        assertEquals("i1v1-pdssd", updateResult.newVolumeId());
        assertEquals(DEVICE_NAME_PREFIX + "i1v1-pdssd", updateResult.newDevice());
        assertEquals(150, updateResult.newSize());
    }

    @Test
    void changeDiskTypeResumesAtDetachWithoutReSnapshottingAndDeletesOldOnlyAfterAttach() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        // The new disk is already READY but the old disk is still attached: resume at detach, never re-create the disk,
        // and never delete the old disk before the new one is attached (the data-loss guard).
        when(gcpDiskUpdateRetryService.resolveDiskTypeChangeResumePoint(any(GcpDiskAttachmentParameters.class),
                any(GcpDiskAttachmentParameters.class), anyString()))
                .thenReturn(new DiskTypeChangeResumePoint(ResumeAction.RESUME_AT_DETACH, Optional.of(new Disk().setSizeGb(150L))));
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        Map<String, VolumeUpdateResult> result =
                underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource));

        // no snapshot / disk creation on resume
        verify(gcpDiskUpdateRetryService, never()).createSnapshot(any(GcpSnapshotParameters.class), any(Snapshot.class), any(GcpContext.class));
        verify(gcpDiskUpdateRetryService, never()).insertDisk(any(GcpCreateDiskParameters.class));
        // detach old -> attach new -> delete old (in that order): the old disk is deleted only after the new disk is attached
        InOrder inOrder = inOrder(gcpDiskUpdateRetryService);
        inOrder.verify(gcpDiskUpdateRetryService).detachDiskFromInstance(any(GcpDiskAttachmentParameters.class), eq(gcpContext));
        ArgumentCaptor<GcpDiskAttachmentParameters> attachCaptor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        inOrder.verify(gcpDiskUpdateRetryService).attachDiskToInstance(attachCaptor.capture(), any(AttachedDisk.class), eq(gcpContext));
        ArgumentCaptor<GcpDiskAttachmentParameters> deleteCaptor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        inOrder.verify(gcpDiskUpdateRetryService).deleteDisk(deleteCaptor.capture(), eq(gcpContext));
        inOrder.verify(gcpDiskUpdateRetryService).deleteSnapshot(any(GcpSnapshotParameters.class), eq(gcpContext));
        assertEquals("i1v1-pdssd", attachCaptor.getValue().diskName(), "The new disk must be the one attached");
        assertEquals("i1v1", deleteCaptor.getValue().diskName(), "The old disk is deleted, only after the new one is attached");
        assertEquals(150, result.get("i1v1").newSize());
    }

    @Test
    void changeDiskTypeResumesAtAttachAndDoesNotDeleteOldDiskBeforeAttaching() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        // The new disk is READY and the old disk already detached: resume at attach. Neither disk is attached right now,
        // so the old disk must not be deleted before the new one is attached (the data-loss guard).
        when(gcpDiskUpdateRetryService.resolveDiskTypeChangeResumePoint(any(GcpDiskAttachmentParameters.class),
                any(GcpDiskAttachmentParameters.class), anyString()))
                .thenReturn(new DiskTypeChangeResumePoint(ResumeAction.RESUME_AT_ATTACH, Optional.of(new Disk().setSizeGb(150L))));
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        Map<String, VolumeUpdateResult> result =
                underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource));

        // no snapshot / disk creation and no detach on resume
        verify(gcpDiskUpdateRetryService, never()).createSnapshot(any(GcpSnapshotParameters.class), any(Snapshot.class), any(GcpContext.class));
        verify(gcpDiskUpdateRetryService, never()).insertDisk(any(GcpCreateDiskParameters.class));
        verify(gcpDiskUpdateRetryService, never()).detachDiskFromInstance(any(GcpDiskAttachmentParameters.class), any(GcpContext.class));
        // attach new -> delete old (in that order): the old disk is deleted only after the new disk is attached
        InOrder inOrder = inOrder(gcpDiskUpdateRetryService);
        ArgumentCaptor<GcpDiskAttachmentParameters> attachCaptor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        inOrder.verify(gcpDiskUpdateRetryService).attachDiskToInstance(attachCaptor.capture(), any(AttachedDisk.class), eq(gcpContext));
        ArgumentCaptor<GcpDiskAttachmentParameters> deleteCaptor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        inOrder.verify(gcpDiskUpdateRetryService).deleteDisk(deleteCaptor.capture(), eq(gcpContext));
        inOrder.verify(gcpDiskUpdateRetryService).deleteSnapshot(any(GcpSnapshotParameters.class), eq(gcpContext));
        assertEquals("i1v1-pdssd", attachCaptor.getValue().diskName(), "The new disk must be the one attached");
        assertEquals("i1v1", deleteCaptor.getValue().diskName(), "The old disk is deleted, only after the new one is attached");
        assertEquals(150, result.get("i1v1").newSize());
    }

    @Test
    void changeDiskTypeRecreatesLeftoverNewDiskThenRunsFullMigration() throws Exception {
        mockContext();
        mockExecutorExecutesInline();
        // A non-READY leftover new disk of ours exists: it must be deleted, then a full snapshot -> create -> swap runs.
        when(gcpDiskUpdateRetryService.resolveDiskTypeChangeResumePoint(any(GcpDiskAttachmentParameters.class),
                any(GcpDiskAttachmentParameters.class), anyString()))
                .thenReturn(new DiskTypeChangeResumePoint(ResumeAction.RECREATE_NEW_DISK, Optional.of(new Disk().setSizeGb(150L))));
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "pd-standard",
                CloudVolumeUsageType.GENERAL);
        CloudResource resource = createTypeChangeResource("instance1", "master", volume);
        CloudStack cloudStack = mockCloudStackWithTemplate("master");

        Map<String, VolumeUpdateResult> result =
                underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 0, cloudStack, List.of(resource));

        // the unusable leftover new disk is deleted first, then the full migration re-creates it
        InOrder inOrder = inOrder(gcpDiskUpdateRetryService);
        ArgumentCaptor<GcpDiskAttachmentParameters> deleteCaptor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        inOrder.verify(gcpDiskUpdateRetryService).deleteDisk(deleteCaptor.capture(), eq(gcpContext));
        inOrder.verify(gcpDiskUpdateRetryService).createSnapshot(any(GcpSnapshotParameters.class), any(Snapshot.class), eq(gcpContext));
        inOrder.verify(gcpDiskUpdateRetryService).insertDisk(any(GcpCreateDiskParameters.class));
        inOrder.verify(gcpDiskUpdateRetryService).detachDiskFromInstance(any(GcpDiskAttachmentParameters.class), eq(gcpContext));
        inOrder.verify(gcpDiskUpdateRetryService).attachDiskToInstance(any(GcpDiskAttachmentParameters.class), any(AttachedDisk.class), eq(gcpContext));
        assertEquals("i1v1-pdssd", deleteCaptor.getValue().diskName(), "The leftover new disk must be the one deleted before recreation");
        // the full migration reports the freshly computed size, not the leftover disk's size
        assertEquals("i1v1-pdssd", result.get("i1v1").newVolumeId());
        assertEquals(100, result.get("i1v1").newSize());
    }

    @Test
    void createVolumesRejectsLocalSsd() {
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, "/dev/sdc", 100, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.createVolumes(authenticatedContext, mock(Group.class), volumeRequest, mock(CloudStack.class), 1, List.of()));
        assertEquals("Local SSD volumes cannot be created via add volumes on GCP.", exception.getMessage());
    }

    @Test
    void createVolumesCreatesDisksAndAddsVolumesToAttributes() throws Exception {
        mockContext();
        mockExecutorRunsInline();
        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder().withVolumes(new ArrayList<>()).build();
        CloudResource resource = createVolumeSetResource("instance1", attributes);
        resource.setStatus(CommonStatus.REQUESTED);
        VolumeSetAttributes.Volume vol0 = new VolumeSetAttributes.Volume("d0", "/dev/disk/by-id/google-d0", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume vol1 = new VolumeSetAttributes.Volume("d1", "/dev/disk/by-id/google-d1", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);
        List<GcpDiskCreationSpec> specs = List.of(
                new GcpDiskCreationSpec(resource, vol0, new Disk().setName("d0"), ZONE),
                new GcpDiskCreationSpec(resource, vol1, new Disk().setName("d1"), ZONE));
        Group group = mock(Group.class);
        CloudStack cloudStack = mock(CloudStack.class);
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, "/dev/sdc", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);
        when(gcpDiskUpdateService.resolveVolumeSets(eq(group), eq(authenticatedContext), anyList())).thenReturn(List.of(resource));
        when(gcpDiskUpdateService.planDisks(eq(authenticatedContext), eq(group), eq(volumeRequest), eq(cloudStack), eq(2), anyList(), eq(compute)))
                .thenReturn(new GcpDiskPlan(specs, List.of()));
        when(gcpDiskUpdateRetryService.insertDisk(any(GcpCreateDiskParameters.class))).thenReturn(Optional.of(resource));

        List<CloudResource> result = underTest.createVolumes(authenticatedContext, group, volumeRequest, cloudStack, 2, List.of(resource));

        assertEquals(List.of(resource), result);
        assertEquals(List.of(vol0, vol1), attributes.getVolumes());
        assertEquals(CommonStatus.CREATED, resource.getStatus());
        verify(gcpDiskUpdateRetryService, times(2)).insertDisk(any(GcpCreateDiskParameters.class));
        // all inserts are polled together once, after the futures complete
        verify(gcpDiskUpdateRetryService).pollDiskOperations(eq(authenticatedContext), eq(gcpContext), anyList());
        verify(gcpDiskUpdateRetryService, never()).deleteDisk(any(GcpCreateDiskParameters.class));
    }

    @Test
    void createVolumesReusesOrphanVolumesWithoutCreating() throws Exception {
        mockContext();
        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder().withVolumes(new ArrayList<>()).build();
        CloudResource resource = createVolumeSetResource("instance1", attributes);
        resource.setStatus(CommonStatus.REQUESTED);
        VolumeSetAttributes.Volume reusedVolume = new VolumeSetAttributes.Volume("orphan-1", "/dev/disk/by-id/google-orphan-1", 100, "pd-ssd",
                CloudVolumeUsageType.GENERAL);
        Group group = mock(Group.class);
        CloudStack cloudStack = mock(CloudStack.class);
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, "/dev/sdc", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);
        when(gcpDiskUpdateService.resolveVolumeSets(eq(group), eq(authenticatedContext), anyList())).thenReturn(List.of(resource));
        when(gcpDiskUpdateService.planDisks(eq(authenticatedContext), eq(group), eq(volumeRequest), eq(cloudStack), eq(1), anyList(), eq(compute)))
                .thenReturn(new GcpDiskPlan(List.of(), List.of(new GcpReusedDisk(resource, reusedVolume))));

        List<CloudResource> result = underTest.createVolumes(authenticatedContext, group, volumeRequest, cloudStack, 1, List.of(resource));

        assertEquals(List.of(resource), result);
        assertEquals(List.of(reusedVolume), attributes.getVolumes());
        assertEquals(CommonStatus.CREATED, resource.getStatus());
        verify(gcpDiskUpdateRetryService, never()).insertDisk(any(GcpCreateDiskParameters.class));
    }

    @Test
    void createVolumesRollsBackCreatedDisksWhenOneFails() throws Exception {
        mockContext();
        mockExecutorRunsInline();
        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder().withVolumes(new ArrayList<>()).build();
        CloudResource resource = createVolumeSetResource("instance1", attributes);
        VolumeSetAttributes.Volume vol0 = new VolumeSetAttributes.Volume("d0", "/dev/disk/by-id/google-d0", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume vol1 = new VolumeSetAttributes.Volume("d1", "/dev/disk/by-id/google-d1", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);
        List<GcpDiskCreationSpec> specs = List.of(
                new GcpDiskCreationSpec(resource, vol0, new Disk().setName("d0"), ZONE),
                new GcpDiskCreationSpec(resource, vol1, new Disk().setName("d1"), ZONE));
        Group group = mock(Group.class);
        CloudStack cloudStack = mock(CloudStack.class);
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, "/dev/sdc", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);
        when(gcpDiskUpdateService.resolveVolumeSets(eq(group), eq(authenticatedContext), anyList())).thenReturn(List.of(resource));
        when(gcpDiskUpdateService.planDisks(eq(authenticatedContext), eq(group), eq(volumeRequest), eq(cloudStack), eq(2), anyList(), eq(compute)))
                .thenReturn(new GcpDiskPlan(specs, List.of()));
        when(gcpDiskUpdateRetryService.insertDisk(any(GcpCreateDiskParameters.class))).thenAnswer(invocation -> {
            GcpCreateDiskParameters params = invocation.getArgument(0);
            if ("d1".equals(params.diskName())) {
                throw new IOException("boom");
            }
            return Optional.of(resource);
        });

        GcpContext deleteContext = mock(GcpContext.class);
        when(gcpContextBuilder.contextInit(cloudContext, authenticatedContext, null, false)).thenReturn(deleteContext);
        when(gcpDiskUpdateRetryService.deleteDisk(any(GcpCreateDiskParameters.class))).thenReturn(Optional.empty());

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.createVolumes(authenticatedContext, group, volumeRequest, cloudStack, 2, List.of(resource)));

        assertTrue(exception.getMessage().contains("d1"));
        assertTrue(exception.getMessage().contains("boom"), "The aggregated exception should surface the underlying GCP failure cause");
        assertTrue(attributes.getVolumes().isEmpty(), "No volumes should be added to the resource on an all-or-nothing failure");
        verify(gcpDiskUpdateRetryService, times(2)).insertDisk(any(GcpCreateDiskParameters.class));
        // every disk submitted in the call is rolled back (not only the ones that polled clean)
        verify(gcpDiskUpdateRetryService, times(2)).deleteDisk(any(GcpCreateDiskParameters.class));
        // the rollback deletions are polled against a build=false delete context so the poll reports DELETED
        verify(gcpDiskUpdateRetryService).pollDiskOperations(eq(authenticatedContext), eq(deleteContext), anyList());
    }

    private CloudResource createDiskResourceWithAttributeZone(String volumeId, int size) {
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(volumeId, "/dev/sdb", size, "pd-ssd", CloudVolumeUsageType.GENERAL);
        CloudResource cloudResource = CloudResource.builder()
                .withInstanceId("instance1")
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(new HashMap<>())
                .build();
        cloudResource.setTypedAttributes(new VolumeSetAttributes.Builder()
                .withAvailabilityZone(ZONE)
                .withVolumes(List.of(volume))
                .build());
        return cloudResource;
    }

    private CloudResource createDiskResourceWithoutZone(String volumeId, int size) {
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(volumeId, "/dev/sdb", size, "pd-ssd", CloudVolumeUsageType.GENERAL);
        CloudResource cloudResource = CloudResource.builder()
                .withInstanceId("instance1")
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(new HashMap<>())
                .build();
        cloudResource.setTypedAttributes(new VolumeSetAttributes.Builder()
                .withVolumes(List.of(volume))
                .build());
        return cloudResource;
    }

    private CloudResource createZonedDiskResource(String volumeId, int size) {
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(volumeId, "/dev/sdb", size, "pd-ssd", CloudVolumeUsageType.GENERAL);
        CloudResource cloudResource = CloudResource.builder()
                .withInstanceId("instance1")
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withAvailabilityZone(ZONE)
                .withParameters(new HashMap<>())
                .build();
        cloudResource.setTypedAttributes(createVolumeSetAttributes(List.of(volume)));
        return cloudResource;
    }

    private CloudResource createTypeChangeResource(String instanceId, String groupName, VolumeSetAttributes.Volume... volumes) {
        CloudResource cloudResource = CloudResource.builder()
                .withInstanceId(instanceId)
                .withGroup(groupName)
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(new HashMap<>())
                .build();
        cloudResource.setTypedAttributes(new VolumeSetAttributes.Builder()
                .withAvailabilityZone(ZONE)
                .withDeleteOnTermination(Boolean.TRUE)
                .withVolumes(new ArrayList<>(List.of(volumes)))
                .build());
        return cloudResource;
    }

    private CloudStack mockCloudStackWithTemplate(String groupName) {
        CloudStack cloudStack = mock(CloudStack.class);
        Group group = mock(Group.class);
        when(group.getName()).thenReturn(groupName);
        when(group.getReferenceInstanceTemplate()).thenReturn(mock(InstanceTemplate.class));
        when(cloudStack.getGroups()).thenReturn(List.of(group));
        return cloudStack;
    }

    private void mockExecutorExecutesInline() {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(intermediateBuilderExecutor).execute(any(Runnable.class));
        // Default: a clean start (no prior attempt). Resume-branch tests override this per case.
        lenient().when(gcpDiskUpdateRetryService.resolveDiskTypeChangeResumePoint(any(GcpDiskAttachmentParameters.class),
                        any(GcpDiskAttachmentParameters.class), anyString()))
                .thenReturn(new DiskTypeChangeResumePoint(ResumeAction.FULL_MIGRATION, Optional.empty()));
    }

    @Test
    void testGetVolumeDeviceMappingByInstance() {
        Map<String, Map<String, String>> result = underTest.getVolumeDeviceMappingByInstance(null, null, createDiskResources());
        assertEquals(Map.ofEntries(
                Map.entry("i1v1", "/dev/disk/by-id/google-local-nvme-ssd-0"),
                Map.entry("i1v2", "/dev/disk/by-id/google-local-nvme-ssd-1"),
                Map.entry("i1v3", "/dev/disk/by-id/google-local-nvme-ssd-2")), result.get("instance1"));
        assertEquals(Map.ofEntries(
                Map.entry("i2v1", "/dev/disk/by-id/google-i2v1"),
                Map.entry("i2v2", "/dev/disk/by-id/google-i2v2"),
                Map.entry("i2v3", "/dev/disk/by-id/google-i2v3"),
                Map.entry("i2v4", "/dev/disk/by-id/google-local-nvme-ssd-0")), result.get("instance2"));
        assertTrue(result.get("instance3").isEmpty());
    }

    private List<CloudResource> createDiskResources() {
        List<CloudResource> resources = new ArrayList<>();
        VolumeSetAttributes.Volume i1v1 = new VolumeSetAttributes.Volume("i1v1", "/dev/sdc", 10, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i1v2 = new VolumeSetAttributes.Volume("i1v2", "/dev/sdd", 10, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i1v3 = new VolumeSetAttributes.Volume("i1v3", "/dev/sde", 10, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        CloudResource volumeSetResource1 = createVolumeSetResource("instance1", createVolumeSetAttributes(List.of(i1v1, i1v2, i1v3)));

        VolumeSetAttributes.Volume i2v1 = new VolumeSetAttributes.Volume("i2v1", "/dev/sdc", 10, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i2v2 = new VolumeSetAttributes.Volume("i2v2", "/dev/sdd", 10, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i2v3 = new VolumeSetAttributes.Volume("i2v3", "/dev/sde", 10, "HDD", CloudVolumeUsageType.DATABASE);
        VolumeSetAttributes.Volume i2v4 = new VolumeSetAttributes.Volume("i2v4", "/dev/sdf", 10, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        CloudResource volumeSetResource2 = createVolumeSetResource("instance2", createVolumeSetAttributes(List.of(i2v1, i2v2, i2v3, i2v4)));

        CloudResource volumeSetResource3 = createVolumeSetResource("instance3", createVolumeSetAttributes(List.of()));

        resources.add(volumeSetResource1);
        resources.add(volumeSetResource2);
        resources.add(volumeSetResource3);
        return resources;
    }

    private CloudResource createVolumeSetResource(String instanceId, VolumeSetAttributes volumeSetAttributes) {
        CloudResource cloudResource = CloudResource.builder()
                .withInstanceId(instanceId)
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(new HashMap<>())
                .build();
        cloudResource.setTypedAttributes(volumeSetAttributes);

        return cloudResource;
    }

    private VolumeSetAttributes createVolumeSetAttributes(List<VolumeSetAttributes.Volume> volumes) {
        return new VolumeSetAttributes.Builder()
                .withAvailabilityZone(ZONE)
                .withVolumes(volumes)
                .build();
    }

    @Test
    void getVolumeInfoFromResourceVolumeForLocalSsd() {
        VolumeSetAttributes.Volume vol = new VolumeSetAttributes.Volume("i2v1", "/dev/disk/by-id/google-abc", 10, "local-ssd", CloudVolumeUsageType.GENERAL);
        VolumeInfo volumeInfo = underTest.getVolumeInfoFromResourceVolume(vol);
        assertEquals("abc", volumeInfo.getId());
        assertEquals("/dev/disk/by-id/google-abc", volumeInfo.getDevice());
        assertEquals(10, volumeInfo.getSize());
        assertFalse(volumeInfo.isDatabaseType());
    }

    @Test
    void getVolumeInfoFromResourceVolumeForNonEphemeralDevice() {
        VolumeSetAttributes.Volume vol = new VolumeSetAttributes.Volume("i2v1", "/dev/disk/by-id/google-abc", 10, "HDD", CloudVolumeUsageType.DATABASE);
        VolumeInfo volumeInfo = underTest.getVolumeInfoFromResourceVolume(vol);
        assertEquals("i2v1", volumeInfo.getId());
        assertEquals("/dev/disk/by-id/google-abc", volumeInfo.getDevice());
        assertEquals(10, volumeInfo.getSize());
        assertTrue(volumeInfo.isDatabaseType());
    }

    @Test
    void testDescribeAttachedVolumes() throws IOException {
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn(PROJECT_ID);

        CloudStack cloudStack = mockCloudStack();
        Instance instance1 = createInstanceWithDisks("instance1",
                bootDisk(),
                persistentDisk("i1v0", 100L));
        Instance instance2 = createInstanceWithDisks("instance2",
                bootDisk(),
                persistentDisk("i2v0", 300L),
                localSsdDisk("local-ssd-0", 375L));
        when(gcpInstanceRetrievalService.getInstance(eq(compute), eq(PROJECT_ID), eq(ZONE), eq("instance1"))).thenReturn(instance1);
        when(gcpInstanceRetrievalService.getInstance(eq(compute), eq(PROJECT_ID), eq(ZONE), eq("instance2"))).thenReturn(instance2);

        Map<String, List<VolumeRecord>> result = underTest.describeAttachedVolumes(authenticatedContext, cloudStack,
                List.of("instance1", "instance2"));

        assertEquals(2, result.size());
        assertEquals(1, result.get("instance1").size());
        assertEquals("i1v0", result.get("instance1").get(0).id());
        assertEquals("/dev/disk/by-id/google-i1v0", result.get("instance1").get(0).device());
        assertEquals(100, result.get("instance1").get(0).size());
        assertEquals("PERSISTENT", result.get("instance1").get(0).type());
        assertEquals(2, result.get("instance2").size());
        assertEquals("i2v0", result.get("instance2").get(0).id());
        assertEquals("/dev/disk/by-id/google-i2v0", result.get("instance2").get(0).device());
        assertEquals(300, result.get("instance2").get(0).size());
        assertEquals("PERSISTENT", result.get("instance2").get(0).type());
        assertEquals("local-ssd-0", result.get("instance2").get(1).id());
        assertEquals("/dev/disk/by-id/google-local-ssd-0", result.get("instance2").get(1).device());
        assertEquals(375, result.get("instance2").get(1).size());
        assertEquals("SCRATCH", result.get("instance2").get(1).type());
    }

    @Test
    void testGetAttachedVolumeCountPerInstance() throws IOException {
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(gcpComputeFactory.buildCompute(cloudCredential)).thenReturn(compute);
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn(PROJECT_ID);

        CloudStack cloudStack = mockCloudStack();
        Instance instance1 = createInstanceWithDisks("instance1",
                bootDisk(),
                persistentDisk("i1v0", 100L));
        Instance instance2 = createInstanceWithDisks("instance2",
                bootDisk(),
                persistentDisk("i2v0", 300L),
                localSsdDisk("local-ssd-0", 375L));
        when(gcpInstanceRetrievalService.getInstance(eq(compute), eq(PROJECT_ID), eq(ZONE), eq("instance1"))).thenReturn(instance1);
        when(gcpInstanceRetrievalService.getInstance(eq(compute), eq(PROJECT_ID), eq(ZONE), eq("instance2"))).thenReturn(instance2);

        Map<String, Integer> result = underTest.getAttachedVolumeCountPerInstance(authenticatedContext, cloudStack,
                List.of("instance1", "instance2"));

        assertEquals(2, result.size());
        assertEquals(1, result.get("instance1"));
        assertEquals(2, result.get("instance2"));
    }

    private CloudStack mockCloudStack() {
        CloudStack cloudStack = mock(CloudStack.class);
        CloudInstance instance1 = mock(CloudInstance.class);
        when(instance1.getInstanceId()).thenReturn("instance1");
        when(instance1.getAvailabilityZone()).thenReturn(ZONE);
        CloudInstance instance2 = mock(CloudInstance.class);
        when(instance2.getInstanceId()).thenReturn("instance2");
        when(instance2.getAvailabilityZone()).thenReturn(ZONE);
        Group group = mock(Group.class);
        when(group.getInstances()).thenReturn(List.of(instance1, instance2));
        when(cloudStack.getGroups()).thenReturn(List.of(group));
        return cloudStack;
    }

    private Instance createInstanceWithDisks(String name, AttachedDisk... attachedDisks) {
        Instance instance = new Instance();
        instance.setName(name);
        instance.setDisks(List.of(attachedDisks));
        return instance;
    }

    private AttachedDisk bootDisk() {
        return new AttachedDisk()
                .setBoot(true)
                .setSource("https://www.googleapis.com/compute/v1/projects/test-project/zones/us-central1-a/disks/boot-disk");
    }

    private AttachedDisk persistentDisk(String deviceName, Long sizeGb) {
        return new AttachedDisk()
                .setBoot(false)
                .setDeviceName(deviceName)
                .setDiskSizeGb(sizeGb)
                .setType("PERSISTENT");
    }

    private AttachedDisk localSsdDisk(String deviceName, Long sizeGb) {
        return new AttachedDisk()
                .setBoot(false)
                .setDeviceName(deviceName)
                .setDiskSizeGb(sizeGb)
                .setType("SCRATCH");
    }

    @Test
    void testDetachVolumesDetachesEachNonLocalSsdVolumeAndSkipsLocalSsd() throws Exception {
        GcpContext context = mockGcpContext();
        runSubmittedTasksSynchronously();

        VolumeSetAttributes.Volume i1v1 = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i1v2 = new VolumeSetAttributes.Volume("i1v2", "/dev/sdd", 375, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i2v1 = new VolumeSetAttributes.Volume("i2v1", "/dev/disk/by-id/google-i2v1", 100, "HDD", CloudVolumeUsageType.DATABASE);
        CloudResource resource1 = createVolumeSetResource("instance1", createVolumeSetAttributes(List.of(i1v1, i1v2)));
        CloudResource resource2 = createVolumeSetResource("instance2", createVolumeSetAttributes(List.of(i2v1)));

        underTest.detachVolumes(authenticatedContext, List.of(resource1, resource2));

        ArgumentCaptor<GcpDiskAttachmentParameters> captor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        verify(gcpDiskUpdateRetryService, times(2)).detachDiskFromInstance(captor.capture(), eq(context));
        List<GcpDiskAttachmentParameters> params = captor.getAllValues();
        assertEquals(List.of("i1v1", "i2v1"), params.stream().map(GcpDiskAttachmentParameters::diskName).toList());
        // device name must be the short disk id, not the OS device path
        assertEquals(List.of("i1v1", "i2v1"), params.stream().map(GcpDiskAttachmentParameters::deviceName).toList());
        assertEquals(List.of("instance1", "instance2"), params.stream().map(GcpDiskAttachmentParameters::instanceId).toList());
    }

    @Test
    void testDetachVolumesThrowsWhenAnyDetachFails() throws Exception {
        mockGcpContext();
        runSubmittedTasksSynchronously();
        when(gcpDiskUpdateRetryService.detachDiskFromInstance(any(GcpDiskAttachmentParameters.class), any(GcpContext.class)))
                .thenThrow(new CloudbreakServiceException("boom"));

        VolumeSetAttributes.Volume i1v1 = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "HDD", CloudVolumeUsageType.GENERAL);
        CloudResource resource1 = createVolumeSetResource("instance1", createVolumeSetAttributes(List.of(i1v1)));

        assertThrows(CloudbreakServiceException.class, () -> underTest.detachVolumes(authenticatedContext, List.of(resource1)));
    }

    @Test
    void testDetachVolumesWithOnlyLocalSsdDoesNotCallProvider() throws Exception {
        mockGcpContext();

        VolumeSetAttributes.Volume i1v1 = new VolumeSetAttributes.Volume("i1v1", "/dev/sdc", 375, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        CloudResource resource1 = createVolumeSetResource("instance1", createVolumeSetAttributes(List.of(i1v1)));

        underTest.detachVolumes(authenticatedContext, List.of(resource1));

        verify(intermediateBuilderExecutor, never()).submit(any(Callable.class));
        verify(gcpDiskUpdateRetryService, never()).detachDiskFromInstance(any(), any());
    }

    @Test
    void testAttachVolumesAttachesEachNonLocalSsdVolumeWithShortDiskIdAndSkipsLocalSsd() throws Exception {
        GcpContext context = mockGcpContext();
        runSubmittedTasksSynchronously();

        VolumeSetAttributes.Volume i1v1 = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i1v2 = new VolumeSetAttributes.Volume("i1v2", "/dev/sdd", 375, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i2v1 = new VolumeSetAttributes.Volume("i2v1", "/dev/disk/by-id/google-i2v1", 100, "HDD", CloudVolumeUsageType.DATABASE);
        CloudResource resource1 = createVolumeSetResource("instance1", createVolumeSetAttributes(List.of(i1v1, i1v2)));
        CloudResource resource2 = createVolumeSetResource("instance2", createVolumeSetAttributes(List.of(i2v1)));

        underTest.attachVolumes(authenticatedContext, List.of(resource1, resource2), mock(CloudStack.class));

        ArgumentCaptor<GcpDiskAttachmentParameters> paramsCaptor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        ArgumentCaptor<AttachedDisk> diskCaptor = ArgumentCaptor.forClass(AttachedDisk.class);
        verify(gcpDiskUpdateRetryService, times(2)).attachDiskToInstance(paramsCaptor.capture(), diskCaptor.capture(), eq(context));
        List<GcpDiskAttachmentParameters> params = paramsCaptor.getAllValues();
        assertEquals(List.of("i1v1", "i2v1"), params.stream().map(GcpDiskAttachmentParameters::diskName).toList());
        assertEquals(List.of("instance1", "instance2"), params.stream().map(GcpDiskAttachmentParameters::instanceId).toList());
        List<AttachedDisk> disks = diskCaptor.getAllValues();
        // both device name and source URL must use the short disk id, not the OS device path
        assertEquals(List.of("i1v1", "i2v1"), disks.stream().map(AttachedDisk::getDeviceName).toList());
        assertEquals("https://www.googleapis.com/compute/v1/projects/test-project/zones/us-central1-a/disks/i1v1", disks.get(0).getSource());
        assertEquals("READ_WRITE", disks.get(0).getMode());
        assertFalse(disks.get(0).getBoot());
    }

    @Test
    void testAttachVolumesThrowsWithFirstCauseWhenAnyAttachFails() throws Exception {
        mockGcpContext();
        runSubmittedTasksSynchronously();
        when(gcpDiskUpdateRetryService.attachDiskToInstance(any(GcpDiskAttachmentParameters.class), any(AttachedDisk.class), any(GcpContext.class)))
                .thenThrow(new CloudbreakServiceException("attach boom"));

        VolumeSetAttributes.Volume i1v1 = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "HDD", CloudVolumeUsageType.GENERAL);
        CloudResource resource1 = createVolumeSetResource("instance1", createVolumeSetAttributes(List.of(i1v1)));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.attachVolumes(authenticatedContext, List.of(resource1), mock(CloudStack.class)));
        assertTrue(exception.getMessage().contains("i1v1"));
        assertTrue(exception.getMessage().contains("attach boom"));
    }

    @Test
    void testDeleteVolumesDeletesEachNonLocalSsdVolumeAndSkipsLocalSsd() throws Exception {
        GcpContext context = mockGcpContext();
        runSubmittedTasksSynchronously();

        VolumeSetAttributes.Volume i1v1 = new VolumeSetAttributes.Volume("i1v1", "/dev/disk/by-id/google-i1v1", 100, "HDD", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i1v2 = new VolumeSetAttributes.Volume("i1v2", "/dev/sdd", 375, LOCAL_SSD.value(), CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume i2v1 = new VolumeSetAttributes.Volume("i2v1", "/dev/disk/by-id/google-i2v1", 100, "HDD", CloudVolumeUsageType.DATABASE);
        CloudResource resource1 = createVolumeSetResource("instance1", createVolumeSetAttributes(List.of(i1v1, i1v2)));
        CloudResource resource2 = createVolumeSetResource("instance2", createVolumeSetAttributes(List.of(i2v1)));

        underTest.deleteVolumes(authenticatedContext, List.of(resource1, resource2));

        ArgumentCaptor<GcpDiskAttachmentParameters> captor = ArgumentCaptor.forClass(GcpDiskAttachmentParameters.class);
        verify(gcpDiskUpdateRetryService, times(2)).deleteDisk(captor.capture(), eq(context));
        assertEquals(List.of("i1v1", "i2v1"), captor.getAllValues().stream().map(GcpDiskAttachmentParameters::diskName).toList());
    }

    private GcpContext mockGcpContext() {
        CloudContext cloudContext = mock(CloudContext.class);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        GcpContext context = mock(GcpContext.class);
        when(gcpContextBuilder.contextInit(eq(cloudContext), eq(authenticatedContext), eq(null), eq(false))).thenReturn(context);
        when(context.getCompute()).thenReturn(compute);
        when(context.getProjectId()).thenReturn(PROJECT_ID);
        return context;
    }

    @SuppressWarnings("unchecked")
    private void runSubmittedTasksSynchronously() {
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Object> callable = invocation.getArgument(0);
            try {
                return CompletableFuture.completedFuture(callable.call());
            } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
            }
        });
    }
}
