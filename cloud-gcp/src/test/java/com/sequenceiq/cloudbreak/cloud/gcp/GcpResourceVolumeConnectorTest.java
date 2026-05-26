package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskType.LOCAL_SSD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpCreateDiskParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskCreationSpec;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskPlan;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskUpdateRetryService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResizeDiskParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpReusedDisk;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.VolumeRecord;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.VolumeInfo;

@ExtendWith(MockitoExtension.class)
class GcpResourceVolumeConnectorTest {

    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    @InjectMocks
    private GcpResourceVolumeConnector underTest;

    @Mock
    private GcpComputeFactory gcpComputeFactory;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpContextBuilder gcpContextBuilder;

    @Mock
    private GcpDiskUpdateService gcpDiskUpdateService;

    @Mock
    private GcpDiskUpdateRetryService gcpDiskUpdateRetryService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private GcpContext gcpContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private Compute compute;

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

        underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200, List.of(createZonedDiskResource("i1v1", 100)));

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
    void testUpdateDiskVolumesThrowsForDiskTypeChange() {
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), "pd-ssd", 200, List.of()));
        assertEquals("Changing disk type is not supported on GCP.", exception.getMessage());
    }

    @Test
    void testUpdateDiskVolumesAggregatesFailures() throws Exception {
        mockContextAndExecutor();
        doThrow(new IOException("boom")).when(gcpDiskUpdateRetryService).resizeDisk(any(GcpResizeDiskParameters.class), eq(gcpContext));

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200, List.of(createZonedDiskResource("i1v1", 100))));
        assertTrue(exception.getMessage().contains("i1v1"));
        assertTrue(exception.getMessage().contains("boom"), "The aggregated exception should surface the underlying GCP failure cause");
    }

    @Test
    void testUpdateDiskVolumesAggregatesAllFailureCauses() throws Exception {
        mockContextAndExecutor();
        when(gcpDiskUpdateRetryService.resizeDisk(any(GcpResizeDiskParameters.class), eq(gcpContext))).thenAnswer(invocation -> {
            GcpResizeDiskParameters params = invocation.getArgument(0);
            throw new IOException(params.diskName() + "-cause");
        });

        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1", "i1v2"), null, 200,
                        List.of(createZonedDiskResource("i1v1", 100), createZonedDiskResource("i1v2", 100))));

        assertTrue(exception.getMessage().contains("i1v1 (i1v1-cause)"), "The aggregated exception should surface the first disk's cause");
        assertTrue(exception.getMessage().contains("i1v2 (i1v2-cause)"), "The aggregated exception should surface every failed disk's cause");
    }

    @Test
    void testUpdateDiskVolumesResolvesZoneFromVolumeSetAttributes() throws Exception {
        mockContextAndExecutor();

        underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200,
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
                () -> underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200,
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

        underTest.updateDiskVolumes(authenticatedContext, List.of("i1v1"), null, 200,
                List.of(nonDiskSetResource, createZonedDiskResource("i1v1", 100)));

        ArgumentCaptor<GcpResizeDiskParameters> captor = ArgumentCaptor.forClass(GcpResizeDiskParameters.class);
        verify(gcpDiskUpdateRetryService).resizeDisk(captor.capture(), eq(gcpContext));
        assertEquals("i1v1", captor.getValue().diskName());
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
        cloudResource.setTypedAttributes(createVolumeSetAttributes(List.of(volume)));
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
        when(gcpStackUtil.getComputeInstanceWithId(eq(compute), eq(PROJECT_ID), eq(ZONE), eq("instance1"))).thenReturn(instance1);
        when(gcpStackUtil.getComputeInstanceWithId(eq(compute), eq(PROJECT_ID), eq(ZONE), eq("instance2"))).thenReturn(instance2);

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
}
