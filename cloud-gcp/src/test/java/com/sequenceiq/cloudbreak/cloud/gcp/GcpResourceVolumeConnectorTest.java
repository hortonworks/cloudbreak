package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpDiskType.LOCAL_SSD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.google.api.services.compute.model.Instance;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.client.GcpComputeFactory;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContextBuilder;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskUpdateRetryService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResizeDiskParameters;
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
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private Compute compute;

    @Mock
    private GcpContextBuilder gcpContextBuilder;

    @Mock
    private GcpDiskUpdateRetryService gcpDiskUpdateRetryService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private GcpContext gcpContext;

    @Mock
    private CloudContext cloudContext;

    private void mockContextAndExecutor() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(gcpContextBuilder.contextInit(cloudContext, authenticatedContext, null, true)).thenReturn(gcpContext);
        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn(PROJECT_ID);
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
