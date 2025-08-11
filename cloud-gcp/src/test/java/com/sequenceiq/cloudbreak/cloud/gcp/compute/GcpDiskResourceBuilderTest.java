package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Disks;
import com.google.api.services.compute.Compute.Disks.Delete;
import com.google.api.services.compute.Compute.Disks.Insert;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionService;
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
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GcpDiskResourceBuilderTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String STACK_AZ = "stack_az";

    private static final String RESOURCE_AZ = "resource_az";

    private static final String PROJECT_ID = "projectId";

    private static final String DISK_NAME = "disk";

    @InjectMocks
    private GcpDiskResourceBuilder underTest;

    @Mock
    private Compute compute;

    @Mock
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    @Mock
    private Disks disks;

    @Mock
    private Insert insert;

    @Mock
    private Delete delete;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    private GcpContext context;

    private long privateId;

    private String privateCrn;

    private AuthenticatedContext auth;

    private Group group;

    private List<CloudResource> buildableResource;

    private CloudStack cloudStack;

    private String name;

    private String instanceId;

    private Security security;

    private InstanceAuthentication instanceAuthentication;

    private InstanceTemplate instanceTemplate;

    private CloudInstance cloudInstance;

    @BeforeEach
    void setUp() throws Exception {
        privateCrn = "crn";
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(privateId)
                .withName("testname")
                .withCrn("crn")
                .withPlatform("GCP")
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cloudCredential = new CloudCredential(privateCrn, "credentialname", "account");
        cloudCredential.putParameter("projectId", PROJECT_ID);

        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone(STACK_AZ));
        String serviceAccountId = "serviceAccountId";
        context = new GcpContext(cloudContext.getName(), location, PROJECT_ID, serviceAccountId, compute, false, 30, false);
        List<CloudResource> networkResources =
                Collections.singletonList(CloudResource.builder().withType(ResourceType.GCP_NETWORK).withName("network-test").build());
        context.addNetworkResources(networkResources);

        privateId = 1L;
        name = "master";
        String flavor = "m1.medium";
        instanceId = "SOME_ID";

        cloudInstance = new CloudInstance(instanceId,
                new InstanceTemplate("flavor", "group", 1L, new ArrayList<>(), InstanceStatus.CREATE_REQUESTED,
                        new HashMap<>(), 1L, "img", TemporaryStorage.ATTACHED_VOLUMES, 0L),
                new InstanceAuthentication("pub", "pub", "cb"),
                "subnet1", "az1");

        auth = new AuthenticatedContext(cloudContext, cloudCredential);

        Map<String, Object> params = Map.of();
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs2", "HDD", 1, CloudVolumeUsageType.GENERAL));

        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, emptyList());

        instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        instanceTemplate = new InstanceTemplate(flavor, name, privateId, volumes, InstanceStatus.CREATE_REQUESTED, params,
                0L, "cb-centos66-amb200-2015-05-25", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        group = createGroup(50);

        buildableResource = List.of(CloudResource.builder()
                .withType(ResourceType.GCP_DISK)
                .withStatus(CommonStatus.REQUESTED)
                .withName("disk")
                .withParameters(Map.of())
                .withAvailabilityZone(RESOURCE_AZ)
                .build());

        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "", "default",
                "default-id", new HashMap<>(), "2019-10-24", 1571884856L, null);
        cloudStack = CloudStack.builder()
                .image(image)
                .build();

        Operation operation = new Operation();
        operation.setName("operation");
        operation.setHttpErrorStatusCode(null);

        when(compute.disks()).thenReturn(disks);
        when(disks.insert(anyString(), anyString(), any(Disk.class))).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        when(disks.delete(PROJECT_ID, STACK_AZ, DISK_NAME)).thenReturn(delete);
        when(disks.delete(PROJECT_ID, RESOURCE_AZ, DISK_NAME)).thenReturn(delete);
        when(delete.execute()).thenReturn(operation);
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
        assertEquals(CommonStatus.REQUESTED, resource.getStatus());
        assertEquals("disk", resource.getName());

        assertNotNull(diskCaptor.getValue());
        assertEquals(encryptionKey, diskCaptor.getValue().getDiskEncryptionKey());
    }

    @Test
    void testBuildWithVeryLargeRootVolumeSize() throws Exception {
        int rootVolumeSize = Integer.MAX_VALUE;
        Group group = createGroup(rootVolumeSize);
        List<CloudResource> build = underTest.build(context, cloudInstance, privateId, auth, group, buildableResource, cloudStack);

        assertNotNull(build);
        verify(disks).insert(anyString(), anyString(), argThat(argument -> argument.getSizeGb().equals((long) rootVolumeSize)));
        verify(insert, times(1)).execute();
    }

    @Test
    void testBuildWithVerySmallRootVolumeSize() throws Exception {
        int rootVolumeSize = Integer.MIN_VALUE;
        Group group = createGroup(rootVolumeSize);

        List<CloudResource> build = underTest.build(context, cloudInstance, privateId, auth, group, buildableResource, cloudStack);

        assertNotNull(build);
        verify(disks).insert(anyString(), anyString(), argThat(argument -> argument.getSizeGb().equals((long) rootVolumeSize)));
        verify(insert, times(1)).execute();
    }

    @Test
    void testDeleteWithResourceAz() throws Exception {
        CloudResource disk = underTest.delete(context, auth, buildableResource.get(0));
        assertNotNull(disk);
        verify(disks).delete(PROJECT_ID, RESOURCE_AZ, DISK_NAME);
    }

    @Test
    void testDeleteWithStackAz() throws Exception {
        CloudResource cloudResource = CloudResource.builder().cloudResource(buildableResource.get(0)).withAvailabilityZone(null).build();
        CloudResource disk = underTest.delete(context, auth, cloudResource);
        assertNotNull(disk);
        verify(disks).delete(PROJECT_ID, STACK_AZ, DISK_NAME);
    }

    private Group createGroup(int rootVolumeSize) {
        return Group.builder()
                .withInstances(Collections.singletonList(createDefaultCloudInstance()))
                .withRootVolumeSize(rootVolumeSize)
                .build();
    }

    private CloudInstance createDefaultCloudInstance() {
        return new CloudInstance(instanceId, instanceTemplate, instanceAuthentication, "subnet-1", "az1");
    }
}
