package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
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
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.CommonStatus;
import com.sequenceiq.cloudbreak.common.type.InstanceGroupType;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpAttachedDiskResourceBuilderTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    @InjectMocks
    private GcpAttachedDiskResourceBuilder underTest;

    @Mock
    private Compute compute;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

    @Mock
    private GcpDiskEncryptionService gcpDiskEncryptionService;

    @Mock
    private AsyncTaskExecutor intermediateBuilderExecutor;

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

    private String flavor;

    private String instanceId;

    private List<Volume> volumes;

    private Security security;

    private Map<String, Object> params;

    private Operation operation;

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
        flavor = "m1.medium";
        instanceId = "SOME_ID";

        auth = new AuthenticatedContext(cloudContext, cloudCredential);

        params = Map.of();
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));

        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, emptyList());

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        InstanceTemplate instanceTemplate = new InstanceTemplate(flavor, name, privateId, volumes, InstanceStatus.CREATE_REQUESTED, params,
                0L, "cb-centos66-amb200-2015-05-25");
        CloudInstance cloudInstance =  new CloudInstance(instanceId, instanceTemplate, instanceAuthentication);
        group = new Group(name, InstanceGroupType.CORE, Collections.singletonList(cloudInstance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), 50);

        List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        volumes.add(new VolumeSetAttributes.Volume("1234", "noop", 0, "eph"));

        VolumeSetAttributes attributes = new VolumeSetAttributes("Ireland", true, "", volumes);
        Map<String, Object> params = new HashMap<>();
        params.put(CloudResource.ATTRIBUTES, attributes);
        buildableResource = List.of(CloudResource.builder()
                .type(ResourceType.GCP_DISK)
                .status(CommonStatus.REQUESTED)
                .name("disk")
                .params(params)
                .build());

        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(Collections.emptyList(), null, image, emptyMap(), emptyMap(), null,
                null, null, null, null);

        when(defaultCostTaggingService.prepareDiskTagging()).thenReturn(Map.of());
        when(intermediateBuilderExecutor.submit(any(Callable.class))).thenAnswer(invocation -> {
            Callable<Void> callable = invocation.getArgument(0);
            return new MockFuture(callable);
        });

        operation = new Operation();
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
        assertEquals(CommonStatus.CREATED, resource.getStatus());
        assertEquals("disk", resource.getName());

        assertNotNull(diskCaptor.getValue());
        assertEquals(encryptionKey, diskCaptor.getValue().getDiskEncryptionKey());
    }
}