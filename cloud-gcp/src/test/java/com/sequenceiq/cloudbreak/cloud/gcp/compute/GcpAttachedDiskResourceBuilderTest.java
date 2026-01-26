package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Disks;
import com.google.api.services.compute.Compute.Disks.Insert;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpAttachedDiskResourceBuilderTest {

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private GcpAttachedDiskResourceBuilder underTest;

    @Mock
    private Compute compute;

    @Mock
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

    @Mock
    private Disks disks;

    @Mock
    private Insert insert;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private GcpResourceNameService resourceNameService;

    private GcpContext context;

    private long privateId;

    private AuthenticatedContext auth;

    private Group group;

    private CloudInstance cloudInstance;

    private List<CloudResource> buildableResource;

    private CloudStack cloudStack;

    private String instanceId;

    @BeforeEach
    void setUpBuild() throws Exception {
        String privateCrn = "crn";
        privateId = 1L;
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(privateId)
                .withName("testname")
                .withCrn("crn")
                .withPlatform("GCP")
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cloudCredential = new CloudCredential(privateCrn, "credentialname", "account");
        cloudCredential.putParameter("projectId", "projectId");

        instanceId = "SOME_ID";
        cloudInstance = new CloudInstance(instanceId,
                new InstanceTemplate("flavor", "group", 1L, new ArrayList<>(), InstanceStatus.CREATE_REQUESTED,
                        new HashMap<>(), 1L, "img", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                new InstanceAuthentication("pub", "pub", "cb"),
                "subnet1", "az1");
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));

        context = new GcpContext(cloudContext.getName(), location, "projectId", "serviceAccountId", compute, false, 30, false);
        List<CloudResource> networkResources =
                Collections.singletonList(CloudResource.builder().withType(ResourceType.GCP_NETWORK).withName("network-test").build());
        context.addNetworkResources(networkResources);
        context.addComputeResources(privateId, Collections.emptyList());

        String name = "master";
        String flavor = "m1.medium";

        auth = new AuthenticatedContext(cloudContext, cloudCredential);

        Map<String, Object> params1 = Map.of();
        List<Volume> volumes1 = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs2", "SSD", 2, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs3", GcpPlatformParameters.GcpDiskType.LOCAL_SSD.value(), 3, CloudVolumeUsageType.GENERAL));

        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        Security security = new Security(rules, emptyList());

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        InstanceTemplate instanceTemplate = new InstanceTemplate(flavor, name, privateId, volumes1, InstanceStatus.CREATE_REQUESTED, params1,
                0L, "cb-centos66-amb200-2015-05-25", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        cloudInstance = new CloudInstance(instanceId, instanceTemplate, instanceAuthentication, "subnet-1", "az1");

        group = Group.builder()
                .withName(name)
                .withInstances(Collections.singletonList(cloudInstance))
                .build();

        List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("1234", "noop", 0, "eph", CloudVolumeUsageType.GENERAL));

        VolumeSetAttributes attributes = new VolumeSetAttributes("Ireland", true, "", volumes, 0, "eph");
        Map<String, Object> params = new HashMap<>();
        params.put(CloudResource.ATTRIBUTES, attributes);
        buildableResource = List.of(CloudResource.builder()
                .withType(ResourceType.GCP_DISK)
                .withStatus(CommonStatus.REQUESTED)
                .withName("disk")
                .withParameters(params)
                .build());

        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "", "default",
                "default-id", new HashMap<>(), "2019-10-24", 1571884856L, null);
        cloudStack = CloudStack.builder()
                .image(image)
                .build();

        lenient().when(intermediateBuilderExecutor.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Void> callable = invocation.getArgument(0);
            callable.call();
            return new MockFuture(callable);
        });

        Operation operation = new Operation();
        operation.setName("operation");
        operation.setHttpErrorStatusCode(null);

        lenient().when(compute.disks()).thenReturn(disks);
        lenient().when(disks.insert(anyString(), anyString(), any(Disk.class))).thenReturn(insert);
        lenient().when(insert.execute()).thenReturn(operation);
    }

    @Test
    void testBuildWithDiskEncryption() throws Exception {
        CustomerEncryptionKey encryptionKey = new CustomerEncryptionKey();
        encryptionKey.setRawKey("rawKey==");

        ArgumentCaptor<Disk> diskCaptor = ArgumentCaptor.forClass(Disk.class);
        doAnswer(invocation -> {
            Disk disk = invocation.getArgument(1);
            disk.setDiskEncryptionKey(encryptionKey);
            return invocation;
        }).when(customGcpDiskEncryptionService).addEncryptionKeyToDisk(any(InstanceTemplate.class), diskCaptor.capture());
        List<CloudResource> build = underTest.build(context, cloudInstance, privateId, auth, group, buildableResource, cloudStack);

        assertNotNull(build);
        assertEquals(1, build.size());
        CloudResource resource = build.iterator().next();
        assertEquals(ResourceType.GCP_DISK, resource.getType());
        assertEquals(CommonStatus.CREATED, resource.getStatus());
        assertEquals("disk", resource.getName());

        assertNotNull(diskCaptor.getValue());
        assertEquals(encryptionKey, diskCaptor.getValue().getDiskEncryptionKey());
    }

    @Test
    void testCreateVolumeSetAttributes() {
        when(resourceNameService.attachedDisk(anyString(), anyString(), anyLong(), any(Integer.class)))
                .thenAnswer(inv -> String.format("testname-master-1-%d", inv.getArgument(3, Integer.class)));

        List<CloudResource> resources = underTest.create(context, cloudInstance, privateId, auth, group, null);

        assertEquals(1, resources.size());
        CloudResource resource = resources.get(0);
        assertEquals(ResourceType.GCP_ATTACHED_DISKSET, resource.getType());
        assertEquals("testname-master-1-0", resource.getName());

        VolumeSetAttributes attributes = resource.getParameter(CloudResource.ATTRIBUTES, VolumeSetAttributes.class);
        assertNotNull(attributes);
        assertEquals("az1", attributes.getAvailabilityZone());
        assertTrue(attributes.getDeleteOnTermination());
        assertEquals(3, attributes.getVolumes().size());

        VolumeSetAttributes.Volume vol1 = attributes.getVolumes().get(0);
        assertEquals("testname-master-1-0", vol1.getId());
        assertEquals("/dev/disk/by-id/google-testname-master-1-0", vol1.getDevice());
        assertEquals(1, vol1.getSize());
        assertEquals("HDD", vol1.getType());
        assertEquals(CloudVolumeUsageType.GENERAL, vol1.getCloudVolumeUsageType());

        VolumeSetAttributes.Volume vol2 = attributes.getVolumes().get(1);
        assertEquals("testname-master-1-1", vol2.getId());
        assertEquals("/dev/disk/by-id/google-testname-master-1-1", vol2.getDevice());
        assertEquals(2, vol2.getSize());
        assertEquals("SSD", vol2.getType());
        assertEquals(CloudVolumeUsageType.GENERAL, vol2.getCloudVolumeUsageType());

        VolumeSetAttributes.Volume vol3 = attributes.getVolumes().get(2);
        assertEquals("testname-master-1-2", vol3.getId());
        assertEquals("/dev/disk/by-id/google-local-nvme-ssd-0", vol3.getDevice());
        assertEquals(3, vol3.getSize());
        assertEquals(GcpPlatformParameters.GcpDiskType.LOCAL_SSD.value(), vol3.getType());
        assertEquals(CloudVolumeUsageType.GENERAL, vol3.getCloudVolumeUsageType());

    }
}
