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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
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
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
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

    private GcpContext context;

    private long privateId;

    private String privateCrn;

    private AuthenticatedContext auth;

    private Group group;

    private CloudInstance cloudInstance;

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
    void setUpBuild() throws Exception {
        privateCrn = "crn";
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(privateId)
                .withName("testname")
                .withCrn("crn")
                .withPlatform("GCP")
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cloudCredential = new CloudCredential(privateCrn, "credentialname", "account");
        cloudCredential.putParameter("projectId", "projectId");

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

        privateId = 1L;
        name = "master";
        flavor = "m1.medium";
        instanceId = "SOME_ID";

        auth = new AuthenticatedContext(cloudContext, cloudCredential);

        params = Map.of();
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs2", "HDD", 1, CloudVolumeUsageType.GENERAL));

        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, emptyList());

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        InstanceTemplate instanceTemplate = new InstanceTemplate(flavor, name, privateId, volumes, InstanceStatus.CREATE_REQUESTED, params,
                0L, "cb-centos66-amb200-2015-05-25", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance cloudInstance =  new CloudInstance(instanceId, instanceTemplate, instanceAuthentication, "subnet-1", "az1");
        group = new Group(name, InstanceGroupType.CORE, Collections.singletonList(cloudInstance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(), 50, Optional.empty(), createGroupNetwork(), emptyMap());

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
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default",
                "default-id", new HashMap<>());
        cloudStack = new CloudStack(emptyList(), null, image, emptyMap(), emptyMap(), null,
                null, null, null, null);

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

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }
}
