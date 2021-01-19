package com.sequenceiq.cloudbreak.service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class LoadBalancerConfigServiceTest extends SubnetTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    @Mock
    private Blueprint blueprint;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private LoadBalancerConfigService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetKnoxGatewayWhenKnoxExplicitlyDefined() {
        Set<String> groups = Set.of("master");
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);

        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        Set<String> selectedGroups = underTest.getKnoxGatewayGroups(stack);
        assertEquals(groups, selectedGroups);
    }

    @Test
    public void testGetKnoxGatewayWhenKnoxImplicitlyDefinedByGatewayGroup() {
        Set<String> groups = Set.of("gateway");
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("gateway");
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setInstanceGroups(Set.of(instanceGroup));

        when(blueprint.getBlueprintText()).thenReturn("{}");

        Set<String> selectedGroups = underTest.getKnoxGatewayGroups(stack);
        assertEquals(groups, selectedGroups);
    }

    @Test
    public void testGetKnoxGatewayWhenNoGateway() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("gateway");
        instanceGroup.setInstanceGroupType(InstanceGroupType.CORE);
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);
        stack.setInstanceGroups(Set.of(instanceGroup));

        when(blueprint.getBlueprintText()).thenReturn("{}");

        Set<String> selectedGroups = underTest.getKnoxGatewayGroups(stack);
        assert selectedGroups.isEmpty();
    }

    @Test
    public void testSelectPrivateLoadBalancer() {
        // Loop here to ensure we're really choosing the private load balancer every time, and not just
        // coincidentally choosing it because it shows up first in the set.
        for (int i = 1; i <= 10; i++) {
            Optional<LoadBalancer> loadBalancer = underTest.selectLoadBalancer(createLoadBalancers(), LoadBalancerType.PRIVATE);
            assert loadBalancer.isPresent();
            assert LoadBalancerType.PRIVATE.equals(loadBalancer.get().getType());
        }
    }

    @Test
    public void testSelectPublicLoadBalancer() {
        // Loop here to ensure we're really choosing the private load balancer every time, and not just
        // coincidentally choosing it because it shows up first in the set.
        for (int i = 1; i <= 10; i++) {
            Optional<LoadBalancer> loadBalancer = underTest.selectLoadBalancer(createLoadBalancers(), LoadBalancerType.PUBLIC);
            assert loadBalancer.isPresent();
            assert LoadBalancerType.PUBLIC.equals(loadBalancer.get().getType());
        }
    }

    @Test
    public void testCreateLoadBalancerForDataLakePrivateSubnets() {
        Stack stack = createStack(StackType.DATALAKE);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PRIVATE, loadBalancers.iterator().next().getType());
        });
    }

    @Test
    public void testCreateLoadBalancerForDataLakePublicSubnets() {
        Stack stack = createStack(StackType.DATALAKE);
        DetailedEnvironmentResponse environment = createEnvironment(getPublicCloudSubnet(PUBLIC_ID_1, AZ_1), false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
        });
    }

    @Test
    public void testCreateLoadBalancerForDatahubWithDatalakeEntitlement() {
        Stack stack = createStack(StackType.WORKLOAD);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerForDataLakeEntitlementDisabled() {
        Stack stack = createStack(StackType.DATALAKE);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancersForEndpointGatewayDatalake() {
        Stack stack = createStack(StackType.DATALAKE);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), true);

        when(entitlementService.publicEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));
        });
    }

    @Test
    public void testCreateLoadBalancersForEndpointGatewayDatahub() {
        Stack stack = createStack(StackType.WORKLOAD);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), true);

        when(entitlementService.publicEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));
        });
    }

    @Test
    public void testCreateLoadBalancersForEndpointGatewayDatalakePublicSubnetsOnly() {
        Stack stack = createStack(StackType.DATALAKE);
        DetailedEnvironmentResponse environment = createEnvironment(getPublicCloudSubnet(PUBLIC_ID_1, AZ_1), true);

        when(entitlementService.publicEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
        });
    }

    @Test
    public void testCreateLoadBalancersUnsupportedStackType() {
        Stack stack1 = createStack(StackType.TEMPLATE);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack1, environment);
            assert loadBalancers.isEmpty();
        });

        Stack stack2 = createStack(StackType.LEGACY);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack2, environment);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancersNullEnvironment() {
        Stack stack = createStack(StackType.DATALAKE);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, null);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancersEndpointGatewayNullNetwork() {
        Stack stack = createStack(StackType.DATALAKE);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PUBLIC_ID_1, AZ_1), true);
        environment.setNetwork(null);

        when(entitlementService.publicEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assert loadBalancers.isEmpty();
        });
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

    private Set<LoadBalancer> createLoadBalancers() {
        LoadBalancer privateLoadBalancer = new LoadBalancer();
        privateLoadBalancer.setType(LoadBalancerType.PRIVATE);
        LoadBalancer publicLoadBalancer = new LoadBalancer();
        publicLoadBalancer.setType(LoadBalancerType.PUBLIC);
        return Set.of(privateLoadBalancer, publicLoadBalancer);
    }

    private Stack createStack(StackType type) {
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        Stack stack = new Stack();
        stack.setType(type);
        stack.setCluster(cluster);
        stack.setInstanceGroups(Set.of(instanceGroup));
        return stack;
    }

    private DetailedEnvironmentResponse createEnvironment(CloudSubnet subnet, boolean enableEndpointGateway) {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setSubnetMetas(Map.of("key", subnet));
        if (enableEndpointGateway) {
            network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        }
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setNetwork(network);
        return environment;
    }
}
