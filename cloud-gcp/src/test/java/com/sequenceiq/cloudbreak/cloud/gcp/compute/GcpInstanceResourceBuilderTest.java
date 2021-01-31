package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.CLOUD_STACK_TYPE_PARAMETER;
import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.FREEIPA_STACK_TYPE;
import static com.sequenceiq.cloudbreak.cloud.model.CloudInstance.DISCOVERY_NAME;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
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
import com.google.api.services.compute.model.InstancesStartWithEncryptionKeyRequest;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpDiskEncryptionService;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
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
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;

@RunWith(MockitoJUnitRunner.class)
public class GcpInstanceResourceBuilderTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private long privateId;

    private String privateCrn;

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
    private Insert insert;

    @Mock
    private GcpDiskEncryptionService gcpDiskEncryptionService;

    @Captor
    private ArgumentCaptor<Instance> instanceArg;

    @InjectMocks
    private final GcpInstanceResourceBuilder builder = new GcpInstanceResourceBuilder();

    @Before
    public void setUp() {
        privateId = 0L;
        privateCrn = "crn";
        name = "master";
        flavor = "m1.medium";
        instanceId = "SOME_ID";
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1, CloudVolumeUsageType.GENERAL),
                new Volume("/hadoop/fs2", "HDD", 1, CloudVolumeUsageType.GENERAL));
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, emptyList());
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());
        CloudContext cloudContext = new CloudContext(privateId, "testname", "crn", "GCP", USER_ID, WORKSPACE_ID);
        CloudCredential cloudCredential = new CloudCredential(privateCrn, "credentialname");
        cloudCredential.putParameter("projectId", "projectId");
        String projectId = GcpStackUtil.getProjectId(cloudCredential);
        String serviceAccountId = GcpStackUtil.getServiceAccountId(cloudCredential);
        authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        context = new GcpContext(cloudContext.getName(), location, projectId, serviceAccountId, compute, false, 30, false);
        List<CloudResource> networkResources = Collections.singletonList(new Builder().type(ResourceType.GCP_NETWORK).name("network-test").build());
        context.addNetworkResources(networkResources);
        operation = new Operation();
        operation.setName("operation");
        operation.setHttpErrorStatusCode(null);
        operation.setError(new Operation.Error());
        GcpResourceNameService resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 50);
        ReflectionTestUtils.setField(builder, "resourceNameService", resourceNameService);
        Network network = new Network(null);
        cloudStack = new CloudStack(Collections.emptyList(), network, image, emptyMap(), emptyMap(), null,
                null, null, null, null);
    }

    @Test
    public void isSchedulingPreemptibleTest() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of("preemptible", true));
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

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
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

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
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

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
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

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
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);
        cloudStack = new CloudStack(Collections.singletonList(group), new Network(null), image,
                ImmutableMap.of(CLOUD_STACK_TYPE_PARAMETER, FREEIPA_STACK_TYPE), emptyMap(), null,
                null, null, null, null);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertNull(instanceArg.getValue().getServiceAccounts());
        assertEquals(ipaserver, instanceArg.getValue().getHostname());
    }

    @Test
    public void extraxtServiceAccountWhenServiceEmailNotEmpty() throws Exception {
        // GIVEN
        String email = "service@email.com";
        CloudGcsView cloudGcsView = new CloudGcsView(CloudIdentityType.LOG);
        cloudGcsView.setServiceAccountEmail(email);

        CloudStack cloudStack = new CloudStack(Collections.emptyList(), new Network(null), image,
                emptyMap(), emptyMap(), null, null, null, null,
                new SpiFileSystem("test", FileSystemType.GCS, List.of(cloudGcsView)));

        Group group = newGroupWithParams(ImmutableMap.of(), cloudGcsView);
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertEquals(instanceArg.getValue().getServiceAccounts().get(0).getEmail(), email);
    }

    public Group newGroupWithParams(Map<String, Object> params) {
        return newGroupWithParams(params, null);
    }

    public Group newGroupWithParams(Map<String, Object> params, CloudFileSystemView cloudFileSystemView) {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstance = newCloudInstance(params, instanceAuthentication);
        return new Group(name,
                InstanceGroupType.CORE,
                Collections.singletonList(cloudInstance),
                security,
                null,
                instanceAuthentication,
                instanceAuthentication.getLoginUserName(),
                instanceAuthentication.getPublicKey(),
                50,
                Optional.ofNullable(cloudFileSystemView));
    }

    public CloudInstance newCloudInstance(Map<String, Object> params, InstanceAuthentication instanceAuthentication) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(flavor, name, privateId, volumes, InstanceStatus.CREATE_REQUESTED, params,
                0L, "cb-centos66-amb200-2015-05-25");
        return new CloudInstance(instanceId, instanceTemplate, instanceAuthentication, params);
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
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        when(compute.instances()).thenReturn(instances);
        ArgumentCaptor<Instance> instanceArgumentCaptor = ArgumentCaptor.forClass(Instance.class);
        when(instances.insert(anyString(), anyString(), instanceArgumentCaptor.capture())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

        verify(gcpDiskEncryptionService, times(0)).addEncryptionKeyToDisk(any(InstanceTemplate.class), any(AttachedDisk.class));

        instanceArgumentCaptor.getValue().getDisks().forEach(attachedDisk -> assertNull(attachedDisk.getDiskEncryptionKey()));
    }

    @Test
    public void testInstanceEncryptionWithRawMethodEmptyKey() throws Exception {
        String encryptionKey = "";
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RAW");
        doTestDiskEncryption(encryptionKey, params);
    }

    @Test
    public void testInstanceEncryptionWithRawMethod() throws Exception {
        String encryptionKey = "theKey";
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RAW", InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, encryptionKey);
        doTestDiskEncryption(encryptionKey, params);
    }

    @Test
    public void testInstanceEncryptionWithEmptyMethod() throws Exception {
        String encryptionKey = "";
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name());
        doTestDiskEncryption(encryptionKey, params);
    }

    @Test
    public void testInstanceEncryptionWithRsaMethodEmptyKey() throws Exception {
        String encryptionKey = "";
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RSA");
        doTestDiskEncryption(encryptionKey, params);
    }

    @Test
    public void testInstanceEncryptionWithRsaMethod() throws Exception {
        String encryptionKey = "theKey";
        ImmutableMap<String, Object> params = ImmutableMap.of(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM.name(),
                "keyEncryptionMethod", "RSA", InstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, encryptionKey);
        doTestDiskEncryption(encryptionKey, params);
    }

    private void doTestDiskEncryption(String encryptionKey, ImmutableMap<String, Object> templateParams) throws Exception {
        Group group = newGroupWithParams(templateParams);
        CloudResource requestedDisk = CloudResource.builder()
                .type(ResourceType.GCP_DISK)
                .status(CommonStatus.REQUESTED)
                .name("dasdisk")
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
        }).when(gcpDiskEncryptionService).addEncryptionKeyToDisk(any(InstanceTemplate.class), any(AttachedDisk.class));

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

        verify(gcpDiskEncryptionService, times(1)).addEncryptionKeyToDisk(any(InstanceTemplate.class), any(AttachedDisk.class));

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

        Get get = Mockito.mock(Get.class);
        when(instances.get(anyString(), anyString(), anyString())).thenReturn(get);
        Start start = Mockito.mock(Start.class);
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

        verify(gcpDiskEncryptionService, times(0)).addEncryptionKeyToDisk(any(InstanceTemplate.class), any(Disk.class));
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

        Get get = Mockito.mock(Get.class);
        when(instances.get(anyString(), anyString(), anyString())).thenReturn(get);
        StartWithEncryptionKey start = Mockito.mock(StartWithEncryptionKey.class);
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

        when(gcpDiskEncryptionService.hasCustomEncryptionRequested(any(InstanceTemplate.class))).thenReturn(true);
        when(gcpDiskEncryptionService.createCustomerEncryptionKey(any(InstanceTemplate.class))).thenReturn(encryptionKey);

        CloudVmInstanceStatus vmInstanceStatus = builder.start(context, authenticatedContext, cloudInstance);

        assertEquals(InstanceStatus.IN_PROGRESS, vmInstanceStatus.getStatus());

        verify(gcpDiskEncryptionService, times(1)).createCustomerEncryptionKey(any(InstanceTemplate.class));
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
    public void testPublicKeyWhenHasEmailAtTheEndShouldCutTheEmail() throws Exception {
        String loginName = "cloudbreak";
        String sshKey = "ssh-rsa key cloudbreak@cloudbreak.com";
        String publicKey = builder.getPublicKey(sshKey, loginName);
        Assert.assertEquals("cloudbreak:ssh-rsa key cloudbreak", publicKey);
    }

    @Test
    public void testPublicKeyWhenHasNoEmailAtTheEndShouldCutTheEmail() throws Exception {
        String loginName = "cloudbreak";
        String sshKey = "ssh-rsa key";
        String publicKey = builder.getPublicKey(sshKey, loginName);
        Assert.assertEquals("cloudbreak:ssh-rsa key cloudbreak", publicKey);
    }

    @Test
    public void testPublicKeyWhenHasLotOfSegmentAtTheEndShouldCutTheEmail() throws Exception {
        String loginName = "cloudbreak";
        String sshKey = "ssh-rsa key cloudbreak cloudbreak cloudbreak cloudbreak cloudbreak cloudbreak";
        String publicKey = builder.getPublicKey(sshKey, loginName);
        Assert.assertEquals("cloudbreak:ssh-rsa key cloudbreak", publicKey);
    }

}