package com.sequenceiq.cloudbreak.cloud.gcp.compute;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Instance;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class GcpInstanceResourceBuilderTest {

    private long privateId;
    private String instanceId;
    private String name;
    private String flavor;
    private List<Volume> volumes;
    private Location location;
    private Map<InstanceGroupType, String> userData;
    private Image image;
    private List<SecurityRule> rules;
    private Security security;
    private CloudContext cloudContext;
    private CloudCredential cloudCredential;
    private String projectId;
    private AuthenticatedContext authenticatedContext;
    private GcpResourceNameService resourceNameService;
    private GcpContext context;
    private CloudResource networkResource;
    private List<CloudResource> networkResources;
    private Operation operation;

    @Mock
    private Compute compute;

    @Mock
    private Compute.Instances instances;

    @Mock
    private Compute.Instances.Insert insert;

    @Captor
    private ArgumentCaptor<Instance> instanceArg;

    @InjectMocks
    private GcpInstanceResourceBuilder builder = new GcpInstanceResourceBuilder();

    @Before
    public void setUp() throws Exception {
        privateId = 0L;
        name = "master";
        flavor = "m1.medium";
        instanceId = "SOME_ID";
        volumes = Arrays.asList(new Volume("/hadoop/fs1", "HDD", 1), new Volume("/hadoop/fs2", "HDD", 1));
        rules = Collections.singletonList(new SecurityRule("0.0.0.0/0", new String[]{"22", "443"}, "tcp"));
        security = new Security(rules);
        location = Location.location(Region.region("region"), AvailabilityZone.availabilityZone("az"));
        userData = ImmutableMap.of(InstanceGroupType.CORE, "CORE", InstanceGroupType.GATEWAY, "GATEWAY");
        image = new Image("cb-centos66-amb200-2015-05-25", userData);
        cloudContext = new CloudContext(privateId, "testname", "GCP", "owner");
        cloudCredential = new CloudCredential(privateId, "credentialname", "sshkey", "loginuser");
        cloudCredential.putParameter("projectId", "projectId");
        projectId = GcpStackUtil.getProjectId(cloudCredential);
        authenticatedContext = new AuthenticatedContext(cloudContext, cloudCredential);
        context = new GcpContext(cloudContext.getName(), location, projectId, compute, 30, false);
        networkResources = Arrays.asList(new CloudResource.Builder().type(ResourceType.GCP_NETWORK).name("network-test").build());
        context.addNetworkResources(networkResources);
        operation = new Operation();
        operation.setName("operation");
        operation.setHttpErrorStatusCode(null);
        resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 50);
        ReflectionTestUtils.setField(builder, "resourceNameService", resourceNameService);
    }

    @Test
    public void isSchedulingPreemptibleTest() throws Exception {
        // GIVEN
        Group group = newGroupWithParams(ImmutableMap.of("preemptible", true));
        List<CloudResource> buildableResources = builder.create(context, privateId, authenticatedContext, group, image);
        context.addComputeResources(0, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), instanceArg.capture())).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        List<CloudResource> resources = builder.build(context, privateId, authenticatedContext, group, image, buildableResources);

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
        context.addComputeResources(0, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), instanceArg.capture())).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        List<CloudResource> resources = builder.build(context, privateId, authenticatedContext, group, image, buildableResources);

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
        context.addComputeResources(0, buildableResources);

        // WHEN
        when(compute.instances()).thenReturn(instances);
        when(instances.insert(anyString(), anyString(), instanceArg.capture())).thenReturn(insert);
        when(insert.setPrettyPrint(anyBoolean())).thenReturn(insert);
        when(insert.execute()).thenReturn(operation);
        List<CloudResource> resources = builder.build(context, privateId, authenticatedContext, group, image, buildableResources);

        // THEN
        verify(compute).instances();
        verify(instances).insert(anyString(), anyString(), instanceArg.capture());
        assertFalse(instanceArg.getValue().getScheduling().getPreemptible());
    }

    public Group newGroupWithParams(Map<String, Object> params) {
        InstanceTemplate instanceTemplate = new InstanceTemplate(flavor, name, privateId, volumes, InstanceStatus.CREATE_REQUESTED, params);
        CloudInstance cloudInstance = new CloudInstance(instanceId, instanceTemplate);
        return new Group(name, InstanceGroupType.CORE, Collections.singletonList(cloudInstance), security);
    }

}