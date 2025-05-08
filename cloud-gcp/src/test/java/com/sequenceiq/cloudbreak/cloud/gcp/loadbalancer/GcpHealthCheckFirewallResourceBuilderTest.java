package com.sequenceiq.cloudbreak.cloud.gcp.loadbalancer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Firewall;
import com.google.api.services.compute.model.Operation;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudLoadBalancer;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.TargetGroupPortPair;
import com.sequenceiq.cloudbreak.cloud.template.ResourceNotNeededException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class GcpHealthCheckFirewallResourceBuilderTest {

    @InjectMocks
    private GcpHealthCheckFirewallResourceBuilder underTest;

    @Mock
    private GcpStackUtil gcpStackUtil;

    @Mock
    private GcpLoadBalancerTypeConverter gcpLoadBalancerTypeConverter;

    @Mock
    private Compute compute;

    @Mock
    private GcpResourceNameService gcpResourceNameService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(underTest, "internalHealthCheckCidrs", List.of("sourceRange1"));
        ReflectionTestUtils.setField(underTest, "externalHealthCheckCidrs", List.of("sourceRange2"));
    }

    @Test
    void testCreateSuccess() {
        // Arrange
        GcpContext context = mock(GcpContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudLoadBalancer loadBalancer = mock(CloudLoadBalancer.class);
        Network network = mock(Network.class);

        when(gcpStackUtil.noFirewallRules(network)).thenReturn(false);
        when(loadBalancer.getPortToTargetGroupMapping()).thenReturn(Map.of(
                new TargetGroupPortPair(80, 8080), Set.of(new Group.Builder().withName("group1").build(), Group.builder().withName("group2").build()),
                        new TargetGroupPortPair(80, 8081), Set.of(new Group.Builder().withName("group3").build(), Group.builder().withName("group4").build())));
        when(loadBalancer.getType()).thenReturn(LoadBalancerType.PUBLIC);
        when(context.getName()).thenReturn("test-context");
        when(gcpResourceNameService.loadBalancerWithPort(anyString(), eq(LoadBalancerType.PUBLIC), anyInt()))
                .thenReturn("test-context-EXTERNAL-8080");

        // Act
        List<CloudResource> result = underTest.create(context, auth, loadBalancer, network);

        // Assert
        assertEquals(2, result.size());
        CloudResource resource8080 = result.stream()
                .filter(resource -> resource.getParameter(AbstractGcpLoadBalancerBuilder.HCPORT, Integer.class) == 8080)
                .findFirst()
                .get();
        assertEquals("test-context-EXTERNAL-8080", resource8080.getName());
        assertEquals(ResourceType.GCP_HEALTHCHECK_FIREWALL, resource8080.getType());
        assertEquals(8080, resource8080.getParameter("hcport", Integer.class));
        assertEquals(Set.of("group1", "group2"), resource8080.getParameter(AbstractGcpLoadBalancerBuilder.TRAFFICPORTS, Set.class));

        CloudResource resource8081 = result.stream()
                .filter(resource -> resource.getParameter(AbstractGcpLoadBalancerBuilder.HCPORT, Integer.class) == 8080)
                .findFirst()
                .get();
        assertEquals("test-context-EXTERNAL-8080", resource8081.getName());
        assertEquals(ResourceType.GCP_HEALTHCHECK_FIREWALL, resource8081.getType());
        assertEquals(8080, resource8081.getParameter("hcport", Integer.class));
        assertEquals(Set.of("group1", "group2"), resource8081.getParameter(AbstractGcpLoadBalancerBuilder.TRAFFICPORTS, Set.class));
    }

    @Test
    void testCreateThrowsResourceNotNeededException() {
        // Arrange
        GcpContext context = mock(GcpContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudLoadBalancer loadBalancer = mock(CloudLoadBalancer.class);
        Network network = mock(Network.class);

        when(gcpStackUtil.noFirewallRules(network)).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceNotNeededException.class, () -> underTest.create(context, auth, loadBalancer, network));
    }

    @Test
    void testBuildInternalSharedProject() throws Exception {
        // Arrange
        GcpContext context = mock(GcpContext.class);
        when(context.getProjectId()).thenReturn("test-project");
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudLoadBalancer loadBalancer = mock(CloudLoadBalancer.class);
        CloudStack cloudStack = mock(CloudStack.class);
        when(cloudStack.getNetwork()).thenReturn(mock(Network.class));
        CloudResource buildableResource1 = createCloudResource("testfirewall1", 8080, Set.of("group1", "group2"));
        CloudResource buildableResource2 = createCloudResource("testfirewall2", 8081, Set.of("group3", "group4"));
        when(gcpLoadBalancerTypeConverter.getScheme(loadBalancer)).thenReturn(GcpLoadBalancerScheme.INTERNAL);
        when(context.getCompute()).thenReturn(compute);
        Compute.Firewalls firewalls = mock(Compute.Firewalls.class);
        when(compute.firewalls()).thenReturn(firewalls);
        Compute.Firewalls.Insert insert = mock(Compute.Firewalls.Insert.class);
        when(firewalls.insert(anyString(), any())).thenReturn(insert);
        Operation operation = mock(Operation.class);
        when(operation.getHttpErrorStatusCode()).thenReturn(null);
        when(insert.execute()).thenReturn(operation);
        when(gcpStackUtil.getNetworkUrl(anyString(), anyString())).thenReturn("test-network-url");
        when(gcpStackUtil.getSharedProjectId(cloudStack.getNetwork())).thenReturn("test-shared-project-id");
        when(gcpStackUtil.getCustomNetworkId(cloudStack.getNetwork())).thenReturn("test-custom-network-id");
        when(gcpStackUtil.getNetworkUrl("test-shared-project-id", "test-custom-network-id")).thenReturn("test-network-url");
        when(gcpStackUtil.getGroupClusterTag(eq(auth.getCloudContext()), anyString())).thenAnswer(
                invocation -> invocation.getArgument(1, String.class) + "-tag");

        // Act
        List<CloudResource> result = underTest.build(context, auth, List.of(buildableResource1, buildableResource2), loadBalancer, cloudStack);

        // Assert
        ArgumentCaptor<Firewall> firewallArgumentCaptor = ArgumentCaptor.forClass(Firewall.class);
        verify(firewalls, times(2)).insert(anyString(), firewallArgumentCaptor.capture());
        List<Firewall> firewallList = firewallArgumentCaptor.getAllValues();
        assertEquals(2, firewallList.size());
        Firewall firewall1 = firewallList.get(0);
        assertEquals(List.of("sourceRange1"), firewall1.getSourceRanges());
        Assertions.assertThat(firewall1.getTargetTags()).contains("group1-tag", "group2-tag");
        Firewall firewall2 = firewallList.get(1);
        assertEquals(List.of("sourceRange1"), firewall2.getSourceRanges());
        Assertions.assertThat(firewall2.getTargetTags()).contains("group3-tag", "group4-tag");
        assertEquals(2, result.size());
        assertEquals("testfirewall1", result.get(0).getName());
        assertEquals("testfirewall2", result.get(1).getName());
    }

    @Test
    void testBuildExternalNoSharedProject() throws Exception {
        // Arrange
        GcpContext context = mock(GcpContext.class);
        when(context.getProjectId()).thenReturn("test-project");
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudLoadBalancer loadBalancer = mock(CloudLoadBalancer.class);
        CloudStack cloudStack = mock(CloudStack.class);
        when(cloudStack.getNetwork()).thenReturn(mock(Network.class));
        CloudResource buildableResource1 = createCloudResource("testfirewall1", 8080, Set.of("group1", "group2"));
        CloudResource buildableResource2 = createCloudResource("testfirewall2", 8081, Set.of("group3", "group4"));
        when(gcpLoadBalancerTypeConverter.getScheme(loadBalancer)).thenReturn(GcpLoadBalancerScheme.EXTERNAL);
        when(context.getCompute()).thenReturn(compute);
        Compute.Firewalls firewalls = mock(Compute.Firewalls.class);
        when(compute.firewalls()).thenReturn(firewalls);
        Compute.Firewalls.Insert insert = mock(Compute.Firewalls.Insert.class);
        when(firewalls.insert(anyString(), any())).thenReturn(insert);
        Operation operation1 = mock(Operation.class);
        when(operation1.getName()).thenReturn("operation1");
        when(operation1.getHttpErrorStatusCode()).thenReturn(null);
        Operation operation2 = mock(Operation.class);
        when(operation2.getName()).thenReturn("operation2");
        when(operation2.getHttpErrorStatusCode()).thenReturn(null);
        when(insert.execute()).thenReturn(operation1, operation2);
        when(gcpStackUtil.getNetworkUrl(anyString(), anyString())).thenReturn("test-network-url");
        when(gcpStackUtil.getCustomNetworkId(cloudStack.getNetwork())).thenReturn("test-custom-network-id");
        when(gcpStackUtil.getGroupClusterTag(eq(auth.getCloudContext()), anyString())).thenAnswer(
                invocation -> invocation.getArgument(1, String.class) + "-tag");

        // Act
        List<CloudResource> result = underTest.build(context, auth, List.of(buildableResource1, buildableResource2), loadBalancer, cloudStack);

        // Assert
        ArgumentCaptor<Firewall> firewallArgumentCaptor = ArgumentCaptor.forClass(Firewall.class);
        verify(firewalls, times(2)).insert(anyString(), firewallArgumentCaptor.capture());
        List<Firewall> firewallList = firewallArgumentCaptor.getAllValues();
        assertEquals(2, firewallList.size());
        Firewall firewall1 = firewallList.get(0);
        assertEquals(List.of("sourceRange2"), firewall1.getSourceRanges());
        Assertions.assertThat(firewall1.getTargetTags()).contains("group1-tag", "group2-tag");
        Firewall firewall2 = firewallList.get(1);
        assertEquals(List.of("sourceRange2"), firewall2.getSourceRanges());
        Assertions.assertThat(firewall2.getTargetTags()).contains("group3-tag", "group4-tag");
        assertEquals(2, result.size());
        assertEquals("testfirewall1", result.get(0).getName());
        assertEquals("operation1", result.get(0).getParameter(AbstractGcpLoadBalancerBuilder.OPERATION_ID, String.class));
        assertEquals("testfirewall2", result.get(1).getName());
        assertEquals("operation2", result.get(1).getParameter(AbstractGcpLoadBalancerBuilder.OPERATION_ID, String.class));
    }

    @Test
    void testDeleteSuccess() throws Exception {
        // Arrange
        GcpContext context = mock(GcpContext.class);
        AuthenticatedContext auth = mock(AuthenticatedContext.class);
        CloudResource resource = createCloudResource("firewall", 8080, Set.of());
        Operation operation = mock(Operation.class);

        when(context.getProjectId()).thenReturn("test-project");
        when(resource.getName()).thenReturn("test-firewall");
        when(context.getCompute()).thenReturn(compute);
        Compute.Firewalls firewalls = mock(Compute.Firewalls.class);
        when(compute.firewalls()).thenReturn(firewalls);
        Compute.Firewalls.Delete delete = mock(Compute.Firewalls.Delete.class);
        when(firewalls.delete("test-project", "test-firewall")).thenReturn(delete);
        when(delete.execute()).thenReturn(operation);

        // Act
        CloudResource result = underTest.delete(context, auth, resource);

        // Assert
        assertEquals("test-firewall", result.getName());
        assertEquals(ResourceType.GCP_HEALTHCHECK_FIREWALL, result.getType());
    }

    private CloudResource createCloudResource(String name, Integer hcPort, Set<String> groups) {
        CloudResource buildableResource = mock(CloudResource.class);
        when(buildableResource.getType()).thenReturn(ResourceType.GCP_HEALTHCHECK_FIREWALL);
        when(buildableResource.getStatus()).thenReturn(CommonStatus.CREATED);
        when(buildableResource.getName()).thenReturn(name);
        lenient().when(buildableResource.getParameter(AbstractGcpLoadBalancerBuilder.HCPORT, Integer.class)).thenReturn(hcPort);
        lenient().when(buildableResource.getParameter(AbstractGcpLoadBalancerBuilder.TRAFFICPORTS, Set.class)).thenReturn(groups);
        return buildableResource;
    }
}
