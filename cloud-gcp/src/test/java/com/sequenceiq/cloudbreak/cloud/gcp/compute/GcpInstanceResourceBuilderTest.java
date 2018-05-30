package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.Instances;
import com.google.api.services.compute.Compute.Instances.Insert;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.model.filesystem.FileSystemType;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
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
import com.sequenceiq.cloudbreak.cloud.model.SpiFileSystem;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudGcsView;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class GcpInstanceResourceBuilderTest {

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
    private Insert insert;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

    @Captor
    private ArgumentCaptor<Instance> instanceArg;

    @InjectMocks
    private final GcpInstanceResourceBuilder builder = new GcpInstanceResourceBuilder();

    @Before
    public void setUp() {
        privateId = 0L;
        name = "master";
        flavor = "m1.medium";
        instanceId = "SOME_ID";
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        security = new Security(rules, null);
        Location location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        Map<InstanceGroupType, String> userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id");
        CloudContext cloudContext = new CloudContext(privateId, "testname", "GCP", "owner");
        CloudCredential cloudCredential = new CloudCredential(privateId, "credentialname");
        cloudCredential.putParameter("projectId", "projectId");
        String projectId = GcpStackUtil.getProjectId(cloudCredential);
        String serviceAccountId = GcpStackUtil.getServiceAccountId(cloudCredential);
        authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        context = new GcpContext(cloudContext.getName(), location, projectId, serviceAccountId, compute, false, 30, false);
        List<CloudResource> networkResources = Arrays.asList(new Builder().type(ResourceType.GCP_NETWORK).name("network-test").build());
        context.addNetworkResources(networkResources);
        operation = new Operation();
        operation.setName("operation");
        operation.setHttpErrorStatusCode(null);
        GcpResourceNameService resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 50);
        ReflectionTestUtils.setField(builder, "resourceNameService", resourceNameService);
        cloudStack = new CloudStack(Collections.emptyList(), null, image, emptyMap(), emptyMap(), null,
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
        when(defaultCostTaggingService.prepareInstanceTagging()).thenReturn(new HashMap<>());

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertTrue(instanceArg.getValue().getScheduling().getPreemptible());
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
        when(defaultCostTaggingService.prepareInstanceTagging()).thenReturn(new HashMap<>());

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertFalse(instanceArg.getValue().getScheduling().getPreemptible());
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
        when(defaultCostTaggingService.prepareInstanceTagging()).thenReturn(new HashMap<>());

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertFalse(instanceArg.getValue().getScheduling().getPreemptible());
    }

    @Test
    public void extraxtServiceAccountWhenServiceEmailEmpty() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of());
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        when(defaultCostTaggingService.prepareInstanceTagging()).thenReturn(new HashMap<>());

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertNull(instanceArg.getValue().getServiceAccounts());
    }

    @Test
    public void extraxtServiceAccountWhenServiceEmailNotEmpty() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of());
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0L, buildableResources);

        String email = "service@email.com";
        CloudGcsView cloudGcsView = new CloudGcsView();
        cloudGcsView.setServiceAccountEmail(email);

        CloudStack cloudStack = new CloudStack(Collections.emptyList(), null, image,
                emptyMap(), emptyMap(), null, null, null, null,
                new SpiFileSystem("test", FileSystemType.GCS, false, cloudGcsView));

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), any(Instance.class))).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        when(defaultCostTaggingService.prepareInstanceTagging()).thenReturn(new HashMap<>());

        builder.build(context, privateId, authenticatedContext, group, buildableResources, cloudStack);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertEquals(instanceArg.getValue().getServiceAccounts().get(0).getEmail(), email);
    }

    public Group newGroupWithParams(Map<String, Object> params) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(flavor, name, privateId, volumes, InstanceStatus.CREATE_REQUESTED, params, 0L);
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        CloudInstance cloudInstance = new CloudInstance(instanceId, instanceTemplate, instanceAuthentication);
        return new Group(name, InstanceGroupType.CORE, Collections.singletonList(cloudInstance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), 50);
    }

}