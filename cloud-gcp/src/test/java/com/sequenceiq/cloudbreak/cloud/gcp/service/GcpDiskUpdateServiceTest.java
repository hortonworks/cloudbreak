package com.sequenceiq.cloudbreak.cloud.gcp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.DiskList;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpDiskUpdateServiceTest {

    private static final String PROJECT_ID = "test-project";

    private static final String ZONE = "us-central1-a";

    private static final String STACK_NAME = "stack";

    private static final String FQDN = "host-1.example.com";

    private static final String CREATED_FOR = "host-1_example_com";

    @InjectMocks
    private GcpDiskUpdateService underTest;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpResourceNameService gcpResourceNameService;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    private Compute compute;

    @Test
    void resolveVolumeSetsReturnsExistingResourcesWhenNotEmpty() {
        List<CloudResource> existing = List.of(createVolumeSetResource("instance1", new ArrayList<>()));
        List<CloudResource> result = underTest.resolveVolumeSets(mockGroup(), authenticatedContext, existing);
        assertSame(existing, result);
    }

    @Test
    void resolveVolumeSetsCreatesNewVolumeSetsWhenEmpty() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getName()).thenReturn(STACK_NAME);
        when(gcpResourceNameService.attachedDisk(eq(STACK_NAME), eq("worker"), eq(42L), eq(0))).thenReturn("worker-disk-0");

        List<CloudResource> result = underTest.resolveVolumeSets(mockGroup(), authenticatedContext, List.of());

        assertEquals(1, result.size());
        CloudResource created = result.get(0);
        assertEquals(ResourceType.GCP_ATTACHED_DISKSET, created.getType());
        assertEquals(CommonStatus.REQUESTED, created.getStatus());
        assertEquals("instance1", created.getInstanceId());
        assertEquals(ZONE, created.getAvailabilityZone());
        assertTrue(created.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class).getVolumes().isEmpty());
    }

    @Test
    void planDisksBuildsOneSpecPerRequestedVolumeWhenNoOrphans() throws Exception {
        stubPlanBasics();
        stubDiskList(new DiskList());
        when(gcpResourceNameService.attachedDisk(eq(STACK_NAME), eq("worker"), eq(42L), anyInt()))
                .thenAnswer(invocation -> "worker-disk-" + invocation.getArgument(3));

        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder().withAvailabilityZone(ZONE).withVolumes(new ArrayList<>()).build();
        CloudResource resource = createVolumeSetResource("instance1", attributes);
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, "/dev/sdc", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);

        GcpDiskPlan plan = underTest.planDisks(authenticatedContext, mockGroup(), volumeRequest, mock(CloudStack.class), 2, List.of(resource), compute);

        assertEquals(2, plan.toCreate().size());
        assertTrue(plan.reused().isEmpty());
        assertEquals("worker-disk-0", plan.toCreate().get(0).disk().getName());
        assertEquals("worker-disk-1", plan.toCreate().get(1).disk().getName());
        assertEquals(ZONE, plan.toCreate().get(0).zone());
        assertSame(resource, plan.toCreate().get(0).resource());
        assertTrue(attributes.getVolumes().isEmpty(), "planDisks must not mutate the resource's existing volumes");
    }

    @Test
    void planDisksLabelsCreatedDisksWithCreatedForLabel() throws Exception {
        stubPlanBasics();
        stubDiskList(new DiskList());
        when(gcpResourceNameService.attachedDisk(eq(STACK_NAME), eq("worker"), eq(42L), anyInt())).thenReturn("worker-disk-0");

        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder().withAvailabilityZone(ZONE).withVolumes(new ArrayList<>()).build();
        CloudResource resource = createVolumeSetResource("instance1", attributes);
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, "/dev/sdc", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);

        GcpDiskPlan plan = underTest.planDisks(authenticatedContext, mockGroup(), volumeRequest, mock(CloudStack.class), 1, List.of(resource), compute);

        assertEquals(CREATED_FOR, plan.toCreate().get(0).disk().getLabels().get(GcpConstants.CREATED_FOR_LABEL));
    }

    @Test
    void planDisksReusesUnattachedOrphanAndCreatesRemainder() throws Exception {
        stubPlanBasics();
        DiskList diskList = new DiskList().setItems(List.of(new Disk().setName("orphan-1")));
        stubDiskList(diskList);
        when(gcpResourceNameService.attachedDisk(eq(STACK_NAME), eq("worker"), eq(42L), anyInt())).thenReturn("worker-disk-fresh");

        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder().withAvailabilityZone(ZONE).withVolumes(new ArrayList<>()).build();
        CloudResource resource = createVolumeSetResource("instance1", attributes);
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, "/dev/sdc", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);

        GcpDiskPlan plan = underTest.planDisks(authenticatedContext, mockGroup(), volumeRequest, mock(CloudStack.class), 2, List.of(resource), compute);

        assertEquals(1, plan.reused().size());
        assertEquals("orphan-1", plan.reused().get(0).volume().getId());
        assertEquals(1, plan.toCreate().size());
        assertEquals("worker-disk-fresh", plan.toCreate().get(0).disk().getName());
    }

    @Test
    void planDisksReusesOrphansSpreadAcrossMultiplePages() throws Exception {
        stubPlanBasics();
        DiskList page1 = new DiskList().setItems(List.of(new Disk().setName("orphan-1"))).setNextPageToken("next");
        DiskList page2 = new DiskList().setItems(List.of(new Disk().setName("orphan-2")));
        lenient().when(compute.disks().list(eq(PROJECT_ID), eq(ZONE)).setFilter(anyString()).execute()).thenReturn(page1, page2);

        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder().withAvailabilityZone(ZONE).withVolumes(new ArrayList<>()).build();
        CloudResource resource = createVolumeSetResource("instance1", attributes);
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, "/dev/sdc", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);

        GcpDiskPlan plan = underTest.planDisks(authenticatedContext, mockGroup(), volumeRequest, mock(CloudStack.class), 2, List.of(resource), compute);

        assertEquals(2, plan.reused().size());
        assertEquals(List.of("orphan-1", "orphan-2"), plan.reused().stream().map(r -> r.volume().getId()).toList());
        assertTrue(plan.toCreate().isEmpty());
    }

    @Test
    void planDisksIgnoresAttachedAndAlreadyRecordedDisks() throws Exception {
        stubPlanBasics();
        Disk attached = new Disk().setName("attached-1").setUsers(List.of("projects/p/zones/z/instances/i"));
        Disk alreadyRecorded = new Disk().setName("recorded-1");
        Disk reusable = new Disk().setName("orphan-1");
        stubDiskList(new DiskList().setItems(List.of(attached, alreadyRecorded, reusable)));
        when(gcpResourceNameService.attachedDisk(eq(STACK_NAME), eq("worker"), eq(42L), anyInt())).thenReturn("worker-disk-fresh");

        VolumeSetAttributes.Volume recorded = new VolumeSetAttributes.Volume("recorded-1", "/dev/sdb", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes attributes = new VolumeSetAttributes.Builder().withAvailabilityZone(ZONE)
                .withVolumes(new ArrayList<>(List.of(recorded))).build();
        CloudResource resource = createVolumeSetResource("instance1", attributes);
        VolumeSetAttributes.Volume volumeRequest = new VolumeSetAttributes.Volume(null, "/dev/sdc", 100, "pd-ssd", CloudVolumeUsageType.GENERAL);

        GcpDiskPlan plan = underTest.planDisks(authenticatedContext, mockGroup(), volumeRequest, mock(CloudStack.class), 2, List.of(resource), compute);

        assertEquals(1, plan.reused().size());
        assertEquals("orphan-1", plan.reused().get(0).volume().getId());
        assertEquals(1, plan.toCreate().size());
    }

    private void stubPlanBasics() {
        when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn(PROJECT_ID);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(cloudContext.getName()).thenReturn(STACK_NAME);
        when(gcpLabelUtil.createLabelsFromTags(any())).thenReturn(Map.of());
        when(gcpLabelUtil.transformLabelKeyOrValue(FQDN)).thenReturn(CREATED_FOR);
    }

    private void stubDiskList(DiskList diskList) throws Exception {
        lenient().when(compute.disks().list(eq(PROJECT_ID), eq(ZONE)).setFilter(anyString()).execute()).thenReturn(diskList);
    }

    private Group mockGroup() {
        InstanceTemplate template = new InstanceTemplate("n1-standard-1", "worker", 42L, List.of(), InstanceStatus.CREATED, Map.of(),
                0L, "image", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance cloudInstance = new CloudInstance("instance1", template, null, null, ZONE, Map.of(CloudInstance.FQDN, FQDN));
        return Group.builder()
                .withName("worker")
                .withInstances(List.of(cloudInstance))
                .build();
    }

    private CloudResource createVolumeSetResource(String instanceId, List<VolumeSetAttributes.Volume> volumes) {
        return createVolumeSetResource(instanceId, new VolumeSetAttributes.Builder().withVolumes(volumes).build());
    }

    private CloudResource createVolumeSetResource(String instanceId, VolumeSetAttributes attributes) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(CloudResource.ATTRIBUTES, attributes);
        return CloudResource.builder()
                .withInstanceId(instanceId)
                .withType(ResourceType.GCP_ATTACHED_DISKSET)
                .withStatus(CommonStatus.CREATED)
                .withName("name")
                .withParameters(parameters)
                .build();
    }
}
