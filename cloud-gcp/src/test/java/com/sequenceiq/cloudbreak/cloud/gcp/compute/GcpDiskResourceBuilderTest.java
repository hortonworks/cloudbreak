package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Disks;
import com.google.api.services.compute.Compute.Disks.Insert;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
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
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpDiskResourceBuilderTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private GcpDiskResourceBuilder underTest;

    @Mock
    private Compute compute;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

    @Mock
    private GcpDiskEncryptionService gcpDiskEncryptionService;

    @Mock
    private Disks disks;

    @Mock
    private Insert insert;

    private GcpContext context;

    private long privateId;

    private AuthenticatedContext auth;

    private Group group;

    private List<CloudResource> buildableResource;

    private CloudStack cloudStack;

    private String name;

    private String instanceId;

    private Security security;

    private InstanceAuthentication instanceAuthentication;

    private InstanceTemplate instanceTemplate;

    @BeforeEach
    void setUp() throws Exception {
        CloudContext cloudContext = new CloudContext(privateId, "testname", "GCP", USER_ID, WORKSPACE_ID);
        CloudCredential cloudCredential = new CloudCredential(privateId, "credentialname");
        cloudCredential.putParameter("projectId", "projectId");

        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        String projectId = GcpStackUtil.getProjectId(cloudCredential);
        String serviceAccountId = GcpStackUtil.getServiceAccountId(cloudCredential);

        context = new GcpContext(cloudContext.getName(), location, projectId, serviceAccountId, compute, false, 30, false);
        List<CloudResource> networkResources = Collections.singletonList(new Builder().type(ResourceType.GCP_NETWORK).name("network-test").build());
        context.addNetworkResources(networkResources);

        privateId = 1L;
        name = "master";
        String flavor = "m1.medium";
        instanceId = "SOME_ID";

        auth = new AuthenticatedContext(cloudContext, cloudCredential);

        Map<String, Object> params = Map.of();
        List<Volume> volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));

        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, emptyList());

        instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        instanceTemplate = new InstanceTemplate(flavor, name, privateId, volumes, InstanceStatus.CREATE_REQUESTED, params,
                0L, "cb-centos66-amb200-2015-05-25");
        group = createGroup(50);

        buildableResource = List.of(CloudResource.builder()
                .type(ResourceType.GCP_DISK)
                .status(CommonStatus.REQUESTED)
                .name("disk")
                .params(Map.of())
                .build());

        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(Collections.emptyList(), null, image, emptyMap(), emptyMap(), null,
                null, null, null, null);

        when(defaultCostTaggingService.prepareDiskTagging()).thenReturn(Map.of());

        Operation operation = new Operation();
        operation.setName("operation");
        operation.setHttpErrorStatusCode(null);

        when(compute.disks()).thenReturn(disks);
        when(disks.insert(anyString(), anyString(), any(Disk.class))).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
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
        }).when(gcpDiskEncryptionService).addEncryptionKeyToDisk(any(InstanceTemplate.class), diskCaptor.capture());
        List<CloudResource> build = underTest.build(context, privateId, auth, group, buildableResource, cloudStack);

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
        List<CloudResource> build = underTest.build(context, privateId, auth, group, buildableResource, cloudStack);

        assertNotNull(build);
        verify(disks).insert(anyString(), anyString(), argThat(argument -> argument.getSizeGb().equals((long) rootVolumeSize)));
        verify(insert, times(1)).execute();
    }

    @Test
    void testBuildWithVerySmallRootVolumeSize() throws Exception {
        int rootVolumeSize = Integer.MIN_VALUE;
        Group group = createGroup(rootVolumeSize);

        List<CloudResource> build = underTest.build(context, privateId, auth, group, buildableResource, cloudStack);

        assertNotNull(build);
        verify(disks).insert(anyString(), anyString(), argThat(argument -> argument.getSizeGb().equals((long) rootVolumeSize)));
        verify(insert, times(1)).execute();
    }

    private Group createGroup(int rootVolumeSize) {
        return new Group(name, InstanceGroupType.CORE, Collections.singletonList(createDefaultCloudInstance()), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), rootVolumeSize);
    }

    private CloudInstance createDefaultCloudInstance() {
        return new CloudInstance(instanceId, instanceTemplate, instanceAuthentication);
    }

}