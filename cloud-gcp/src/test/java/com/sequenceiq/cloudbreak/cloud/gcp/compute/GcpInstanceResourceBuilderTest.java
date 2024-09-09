package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.CLOUD_STACK_TYPE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.FREEIPA_STACK_TYPE;
import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.DISCOVERY_NAME;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.InstanceGroups;
import com.google.api.services.compute.Compute.InstanceGroups.AddInstances;
import com.google.api.services.compute.Compute.Instances;
import com.google.api.services.compute.Compute.Instances.Get;
import com.google.api.services.compute.Compute.Instances.Insert;
import com.google.api.services.compute.Compute.Instances.Start;
import com.google.api.services.compute.Compute.Instances.StartWithEncryptionKey;
import com.google.api.services.compute.model.AttachedDisk;
import com.google.api.services.compute.model.CustomerEncryptionKey;
import com.google.api.services.compute.model.CustomerEncryptionKeyProtectedDisk;
import com.google.api.services.compute.model.Disk;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.InstanceGroup;
import com.google.api.services.compute.model.InstancesStartWithEncryptionKeyRequest;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionCreatorService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.CustomGcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpLabelUtil;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudFileSystemView;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
public class GcpInstanceResourceBuilderTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String ENCRYPTION_KEY = "theKey";

    private static final String PROJECT_ID = "projectId";

    private static final String AVAILABILITY_ZONE = "az1";

    @InjectMocks
    private GcpInstanceResourceBuilder builder;

    private long privateId;

    private String instanceId;

    private String name;

    private String flavor;

    private List<Volume> volumes;

    private Image image;

    private CloudStack cloudStack;

    private Security security;

    private AuthenticatedContext authenticatedContext;

    private GcpContext context;

    private Operation operation;

    @Mock
    private Compute compute;

    @Mock
    private Instances instances;

    @Mock
    private InstanceGroups instanceGroups;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpLabelUtil gcpLabelUtil;

    @Mock
    private Insert insert;

    @Mock
    private AddInstances addInstances;

    @Mock
    private CustomGcpDiskEncryptionService customGcpDiskEncryptionService;

    @Mock
    private CustomGcpDiskEncryptionCreatorService customGcpDiskEncryptionCreatorService;

    @Captor
    private ArgumentCaptor<Instance> instanceArg;

    @BeforeEach
    public void setUp() {
        privateId = 0L;
        String privateCrn = "crn";
        name = "master";
        flavor = "m1.medium";
        instanceId = "SOME_ID";
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs2", "HDD", 1, CloudVolumeUsageType.GENERAL));
        List<SecurityRule> rules = singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, emptyList());
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "", "default", "default-id", new HashMap<>(), "2019-10-24",
                1571884856L);
        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(privateId)
                .withName("testname")
                .withCrn("crn")
                .withPlatform("GCP")
                .withWorkspaceId(WORKSPACE_ID)
                .build();
        CloudCredential cloudCredential = new CloudCredential(privateCrn, "credentialname", "account");
        cloudCredential.putParameter("projectId", PROJECT_ID);
        String serviceAccountId = "serviceAccountId";
        lenient().when(gcpStackUtil.getProjectId(cloudCredential)).thenReturn(PROJECT_ID);
        authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        context = new GcpContext(cloudContext.getName(), location, PROJECT_ID, serviceAccountId, compute, false, 30, false);
        List<CloudResource> networkResources =
                singletonList(CloudResource.builder().withType(ResourceType.GCP_NETWORK).withName("network-test").build());
        context.addNetworkResources(networkResources);
        operation = new Operation();
        operation.setName("operation");
        operation.setHttpErrorStatusCode(null);
        operation.setError(new Operation.Error());
        GcpResourceNameService resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 50);
        ReflectionTestUtils.setField(builder, "resourceNameService", resourceNameService);
        Network network = new Network(null);
        cloudStack = CloudStack.builder()
                .network(network)
                .image(image)
                .build();
    }

    @Test
    public void isSchedulingPreemptibleTest() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of("preemptible", true));
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertTrue(instanceArg.getValue().getScheduling().getPreemptible());
        assertNull(instanceArg.getValue().getHostname());
    }

    @Test
    public void isSchedulingNotPreemptibleTest() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of("preemptible", false));
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertFalse(instanceArg.getValue().getScheduling().getPreemptible());
        assertNull(instanceArg.getValue().getHostname());
    }

    @Test
    public void preemptibleParameterNotSetTest() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of());
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertFalse(instanceArg.getValue().getScheduling().getPreemptible());
        assertNull(instanceArg.getValue().getHostname());
    }

    @Test
    public void extraxtServiceAccountWhenServiceEmailEmpty() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of(DISCOVERY_NAME, "idbroker"));
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertNull(instanceArg.getValue().getServiceAccounts());
        assertNull(instanceArg.getValue().getHostname());
    }

    @Test
    public void freeipaHostnameSet() throws Exception {
        // GIVEN
        String ipaserver = "ipaserver";
        Group group = newGroupWithParams(ImmutableMap.of(DISCOVERY_NAME, ipaserver));
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);
        cloudStack = CloudStack.builder()
                .groups(singletonList(group))
                .network(new Network(null))
                .image(image)
                .parameters(ImmutableMap.of(CLOUD_STACK_TYPE_PARAMETER, FREEIPA_STACK_TYPE))
                .build();

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertNull(instanceArg.getValue().getServiceAccounts());
        assertEquals(ipaserver, instanceArg.getValue().getHostname());
    }

    @Test
    public void labelsAndTagsSetCorrectly() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of(DISCOVERY_NAME, "idbroker"));
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);
        Map<String, String> cdpTags = new HashMap<>();
        cdpTags.put("owner", "cdpUser");

        // WHEN
        when(gcpStackUtil.convertGroupName(anyString())).thenReturn("idbroker");
        when(gcpLabelUtil.createLabelsFromTags(any())).thenReturn(cdpTags);

        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertTrue(instanceArg.getValue().getTags().getItems().contains("idbroker"));
        assertFalse(instanceArg.getValue().getTags().getItems().contains("owner"));
        assertEquals("cdpUser", instanceArg.getValue().getLabels().get("owner"));
    }

    @Test
    public void extraxtServiceAccountWhenServiceEmailNotEmpty() throws Exception {
        // GIVEN
        String email = "service@email.com";
        CloudGcsView cloudGcsView = new CloudGcsView(CloudIdentityType.LOG);
        cloudGcsView.setServiceAccountEmail(email);

        CloudStack cloudStack = CloudStack.builder()
                .fileSystem(new SpiFileSystem("test", FileSystemType.GCS, List.of(cloudGcsView)))
                .network(new Network(null))
                .image(image)
                .build();

        Group group = newGroupWithParams(ImmutableMap.of(), cloudGcsView);
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertEquals(instanceArg.getValue().getServiceAccounts().get(0).getEmail(), email);
    }

    @Test
    public void addToInstanceGroupFailsAuth() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of());
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        List<CloudResource> resourcesWithGroup = buildableResources.stream()
                .map(b -> CloudResource.builder().cloudResource(b).withGroup(group.getName()).build())
                .collect(Collectors.toList());
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        Operation addOperation = new Operation();
        addOperation.setName("operation");
        addOperation.setHttpErrorStatusCode(401);
        addOperation.setHttpErrorMessage("Not Authorized");
        addOperation.setError(new Operation.Error());
        CloudResource instanceGroup = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName("test-master-1")
                .withGroup(group.getName())
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();
        context.addGroupResources(group.getName(), singletonList(instanceGroup));
        when(compute.instanceGroups()).thenReturn(instanceGroups);
        when(instanceGroups.addInstances(eq(PROJECT_ID), eq(AVAILABILITY_ZONE), eq("test-master-1"), any())).thenReturn(addInstances);
        InstanceGroups.Get get = mock(InstanceGroups.Get.class);
        get.setInstanceGroup("test-master-1");
        InstanceGroup ig = new InstanceGroup();
        ig.setName("test-master-1");
        when(instanceGroups.get(anyString(), anyString(), anyString())).thenReturn(get);
        when(get.execute()).thenReturn(ig);
        when(addInstances.execute()).thenReturn(addOperation);

        assertThrows(GcpResourceException.class,
                () -> builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, resourcesWithGroup, cloudStack));

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertNull(instanceArg.getValue().getHostname());
    }

    @Test
    public void addInstanceGroupFromUpscale() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of());
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        Operation addOperation = new Operation();
        addOperation.setName("operation");
        CloudResource masterInstanceGroup = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName("test-master-1")
                .withGroup("master")
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();
        CloudResource gatewayInstanceGroup = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName("test-gateway-1")
                .withGroup("gateway")
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();
        CloudResource idBrokerInstanceGroup = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName("test-idbroker-1")
                .withGroup("idbroker")
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();
        CloudResource master0InstanceGroup = CloudResource.builder()
                .withType(ResourceType.GCP_INSTANCE_GROUP)
                .withStatus(CommonStatus.CREATED)
                .withName("test-master0-1")
                .withGroup("master0")
                .withAvailabilityZone(AVAILABILITY_ZONE)
                .build();
        context.addGroupResources(group.getName(), List.of(master0InstanceGroup, gatewayInstanceGroup, masterInstanceGroup, idBrokerInstanceGroup));
        when(compute.instanceGroups()).thenReturn(instanceGroups);
        ArgumentCaptor<String> groupName = ArgumentCaptor.forClass(String.class);
        when(instanceGroups.addInstances(eq(PROJECT_ID), eq(AVAILABILITY_ZONE), groupName.capture(), any())).thenReturn(addInstances);
        InstanceGroups.Get get = mock(InstanceGroups.Get.class);
        InstanceGroup ig = new InstanceGroup();
        ig.setName(masterInstanceGroup.getName());
        get.setInstanceGroup(masterInstanceGroup.getName());
        when(instanceGroups.get(anyString(), anyString(), anyString())).thenReturn(get);
        when(get.execute()).thenReturn(ig);
        when(addInstances.execute()).thenReturn(addOperation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        assertEquals(masterInstanceGroup.getName(), groupName.getValue());
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertNull(instanceArg.getValue().getHostname());
    }

    @Test
    public void noInstanceGroupsExist() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of());
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertNull(instanceArg.getValue().getHostname());
        verifyNoInteractions(addInstances);
    }

    @Test
    public void noSubnetInformationOnInstance() throws Exception {

        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of());
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);
        group.getInstances().get(0).setSubnetId(null);
        when(gcpStackUtil.getSubnetId(any())).thenReturn("default");

        ArgumentCaptor<String> subnetCaptor = ArgumentCaptor.forClass(String.class);
        when(gcpStackUtil.getSubnetUrl(anyString(), anyString(), subnetCaptor.capture()))
                .thenReturn("https://www.googleapis.com/compute/v1/projects/projectId/regions/region/subnetworks/default");

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertEquals("https://www.googleapis.com/compute/v1/projects/projectId/regions/region/subnetworks/default",
                instanceArg.getValue().getNetworkInterfaces().get(0).getSubnetwork());
        assertNull(instanceArg.getValue().getHostname());
        assertEquals(subnetCaptor.getValue(), "default");
    }

    public Group newGroupWithParams(Map<String, Object> params) {
        return newGroupWithParams(params, null);
    }

    public Group newGroupWithParams(Map<String, Object> params, CloudFileSystemView cloudFileSystemView) {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstance = newCloudInstance(params, instanceAuthentication);
        return Group.builder()
                .withName(name)
                .withType(InstanceGroupType.CORE)
                .withInstances(singletonList(cloudInstance))
                .withSecurity(security)
                .withInstanceAuthentication(instanceAuthentication)
                .withLoginUserName(instanceAuthentication.getLoginUserName())
                .withPublicKey(instanceAuthentication.getPublicKey())
                .withIdentity(Optional.ofNullable(cloudFileSystemView))
                .build();
    }

    public CloudInstance newCloudInstance(Map<String, Object> params, InstanceAuthentication instanceAuthentication) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(flavor, name, privateId, volumes, InstanceStatus.CREATE_REQUESTED, params,
                0L, "cb-centos66-amb200-2015-05-25", TemporaryStorage.ATTACHED_VOLUMES, 0L);
        return new CloudInstance(instanceId, instanceTemplate, instanceAuthentication, "subnet-1", AVAILABILITY_ZONE, params);
    }

    @Test
    public void testInstanceEncryptionWithDefault() throws Exception {
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name());
        doTestDefaultDiskEncryption(params);
    }

    @Test
    public void testInstanceEncryptionWithEmptyType() throws Exception {
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, "");
        doTestDefaultDiskEncryption(params);
    }

    public void doTestDefaultDiskEncryption(ImmutableMap<String, Object> params) throws Exception {
        Group group = newGroupWithParams(params);
        List<CloudResource> buildableResources = builder.create(context, group.getInstances().get(0), privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        when(compute.instances()).thenReturn(instances);
        ArgumentCaptor<Instance> instanceArgumentCaptor = ArgumentCaptor.forClass(Instance.class);
        when(instances.insert(anyString(), anyString(), instanceArgumentCaptor.capture())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        verify(customGcpDiskEncryptionService, times(0)).addEncryptionKeyToDisk(any(InstanceTemplate.class), any(AttachedDisk.class));

        instanceArgumentCaptor.getValue().getDisks().forEach(attachedDisk -> assertNull(attachedDisk.getDiskEncryptionKey()));
    }

    @Test
    public void testInstanceEncryptionWithRawMethodEmptyKey() throws Exception {
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RAW");
        doTestDiskEncryption(params);
    }

    @Test
    public void testInstanceEncryptionWithRawMethod() throws Exception {
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RAW", InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY);
        doTestDiskEncryption(params);
    }

    @Test
    public void testInstanceEncryptionWithEmptyMethod() throws Exception {
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name());
        doTestDiskEncryption(params);
    }

    @Test
    public void testInstanceEncryptionWithRsaMethodEmptyKey() throws Exception {
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RSA");
        doTestDiskEncryption(params);
    }

    @Test
    public void testInstanceEncryptionWithRsaMethod() throws Exception {
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RSA", InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, ENCRYPTION_KEY);
        doTestDiskEncryption(params);
    }

    private void doTestDiskEncryption(ImmutableMap<String, Object> templateParams) throws Exception {
        Group group = newGroupWithParams(templateParams);
        CloudResource requestedDisk = CloudResource.builder()
                .withType(ResourceType.GCP_DISK)
                .withStatus(CommonStatus.REQUESTED)
                .withName("dasdisk")
                .build();
        List<CloudResource> buildableResources = List.of(requestedDisk);
        context.addComputeResources(0L, buildableResources);

        when(compute.instances()).thenReturn(instances);

        ArgumentCaptor<Instance> instanceArgumentCaptor = ArgumentCaptor.forClass(Instance.class);
        when(instances.insert(anyString(), anyString(), instanceArgumentCaptor.capture())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        CustomerEncryptionKey customerEncryptionKey = new CustomerEncryptionKey();
        customerEncryptionKey.setRawKey("encodedKey==");
        doAnswer(invocation -> {
            AttachedDisk argument = invocation.getArgument(1);
            argument.setDiskEncryptionKey(customerEncryptionKey);
            return invocation;
        }).when(customGcpDiskEncryptionService).addEncryptionKeyToDisk(any(InstanceTemplate.class), any(AttachedDisk.class));

        builder.build(context, group.getInstances().get(0), privateId, authenticatedContext, group, buildableResources, cloudStack);

        verify(customGcpDiskEncryptionService, times(1)).addEncryptionKeyToDisk(any(InstanceTemplate.class), any(AttachedDisk.class));

        instanceArgumentCaptor.getValue().getDisks().forEach(attachedDisk -> {
            assertNotNull(attachedDisk.getDiskEncryptionKey());
            assertEquals(customerEncryptionKey, attachedDisk.getDiskEncryptionKey());
        });
    }

    @Test
    public void testStartWithDefaultEncryption() throws Exception {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstance = newCloudInstance(Map.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.DEFAULT.name()),
                instanceAuthentication);

        doTestDefaultEncryption(cloudInstance);
    }

    public void doTestDefaultEncryption(CloudInstance cloudInstance) throws IOException {
        when(compute.instances()).thenReturn(instances);

        Get get = mock(Get.class);
        when(instances.get(anyString(), anyString(), anyString())).thenReturn(get);
        Start start = mock(Start.class);
        when(instances.start(anyString(), anyString(), anyString())).thenReturn(start);

        String expectedSource = "google.disk";
        AttachedDisk disk = new AttachedDisk();
        disk.setSource(expectedSource);
        Instance instance = new Instance();
        instance.setDisks(List.of(disk));
        instance.setStatus("TERMINATED");
        when(get.execute()).thenReturn(instance);

        when(start.setPrettyPrint(true)).thenReturn(start);
        when(start.execute()).thenReturn(operation);

        CloudVmInstanceStatus vmInstanceStatus = builder.start(context, authenticatedContext, cloudInstance);

        assertEquals(InstanceStatus.IN_PROGRESS, vmInstanceStatus.getStatus());

        verify(customGcpDiskEncryptionService, times(0)).addEncryptionKeyToDisk(any(InstanceTemplate.class), any(Disk.class));
        verify(instances, times(0))
                .startWithEncryptionKey(anyString(), anyString(), anyString(), any(InstancesStartWithEncryptionKeyRequest.class));
    }

    @Test
    public void testStartWithDefaultEncryptionNoTemplateParams() throws Exception {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstance = newCloudInstance(Map.of(), instanceAuthentication);

        doTestDefaultEncryption(cloudInstance);
    }

    @Test
    public void testStartWithRawEncryptedKey() throws Exception {
        CustomerEncryptionKey customerEncryptionKey = new CustomerEncryptionKey();
        customerEncryptionKey.setRawKey("HelloWorld==");

        Map<String, Object> params = Map.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RAW", InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "Hello World");
        doTestCustomEncryption(params, customerEncryptionKey);
    }

    public void doTestCustomEncryption(Map<String, Object> params, CustomerEncryptionKey encryptionKey) throws IOException {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstance = newCloudInstance(params, instanceAuthentication);
        when(compute.instances()).thenReturn(instances);

        ArgumentCaptor<InstancesStartWithEncryptionKeyRequest> requestCaptor = ArgumentCaptor.forClass(InstancesStartWithEncryptionKeyRequest.class);

        Get get = mock(Get.class);
        when(instances.get(anyString(), anyString(), anyString())).thenReturn(get);
        StartWithEncryptionKey start = mock(StartWithEncryptionKey.class);
        when(instances.startWithEncryptionKey(anyString(), anyString(), anyString(), requestCaptor.capture())).thenReturn(start);

        String expectedSource = "google.disk";
        AttachedDisk disk = new AttachedDisk();
        disk.setSource(expectedSource);
        Instance instance = new Instance();
        instance.setDisks(List.of(disk));
        instance.setStatus("TERMINATED");
        when(get.execute()).thenReturn(instance);

        when(start.setPrettyPrint(true)).thenReturn(start);
        when(start.execute()).thenReturn(operation);

        when(customGcpDiskEncryptionService.hasCustomEncryptionRequested(any(InstanceTemplate.class))).thenReturn(true);
        when(customGcpDiskEncryptionCreatorService.createCustomerEncryptionKey(any(InstanceTemplate.class))).thenReturn(encryptionKey);

        CloudVmInstanceStatus vmInstanceStatus = builder.start(context, authenticatedContext, cloudInstance);

        assertEquals(InstanceStatus.IN_PROGRESS, vmInstanceStatus.getStatus());

        verify(customGcpDiskEncryptionCreatorService, times(1)).createCustomerEncryptionKey(any(InstanceTemplate.class));
        verify(instances, times(0)).start(anyString(), anyString(), anyString());

        InstancesStartWithEncryptionKeyRequest keyRequest = requestCaptor.getValue();
        assertNotNull(keyRequest.getDisks());
        assertEquals(1, keyRequest.getDisks().size());

        CustomerEncryptionKeyProtectedDisk protectedDisk = keyRequest.getDisks().iterator().next();
        assertEquals(encryptionKey, protectedDisk.getDiskEncryptionKey());
        assertEquals(expectedSource, protectedDisk.getSource());
    }

    @Test
    public void testStartWithRsaEncryptedKey() throws Exception {
        CustomerEncryptionKey customerEncryptionKey = new CustomerEncryptionKey();
        customerEncryptionKey.setRawKey("HelloWorld==");

        Map<String, Object> params = Map.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RSA", InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "Hello World");
        doTestCustomEncryption(params, customerEncryptionKey);
    }

    @Test
    public void testStartWithEmptyMethodRsaEncryptedKey() throws Exception {
        CustomerEncryptionKey customerEncryptionKey = new CustomerEncryptionKey();
        customerEncryptionKey.setRawKey("HelloWorld==");

        Map<String, Object> params = Map.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "Hello World");
        doTestCustomEncryption(params, customerEncryptionKey);
    }

    @Test
    public void testPublicKeyWhenHasEmailAtTheEndShouldCutTheEmail() {
        String loginName = "cloudbreak";
        String sshKey = "ssh-rsa key cloudbreak@cloudbreak.com";
        String publicKey = builder.getPublicKey(sshKey, loginName);
        assertEquals("cloudbreak:ssh-rsa key cloudbreak", publicKey);
    }

    @Test
    public void testPublicKeyWhenHasNoEmailAtTheEndShouldCutTheEmail() {
        String loginName = "cloudbreak";
        String sshKey = "ssh-rsa key";
        String publicKey = builder.getPublicKey(sshKey, loginName);
        assertEquals("cloudbreak:ssh-rsa key cloudbreak", publicKey);
    }

    @Test
    public void testPublicKeyWhenHasLotOfSegmentAtTheEndShouldCutTheEmail() {
        String loginName = "cloudbreak";
        String sshKey = "ssh-rsa key cloudbreak cloudbreak cloudbreak cloudbreak cloudbreak cloudbreak";
        String publicKey = builder.getPublicKey(sshKey, loginName);
        assertEquals("cloudbreak:ssh-rsa key cloudbreak", publicKey);
    }
}
