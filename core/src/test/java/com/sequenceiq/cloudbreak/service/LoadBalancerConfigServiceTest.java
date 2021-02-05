package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

public class LoadBalancerConfigServiceTest extends SubnetTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String PRIVATE_DNS = "private.dns";

    private static final String PUBLIC_DNS = "public.dns";

    @Mock
    private Blueprint blueprint;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SubnetSelector subnetSelector;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @InjectMocks
    private LoadBalancerConfigService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Field supportedPlatformsField = ReflectionUtils.findField(LoadBalancerConfigService.class, "supportedPlatforms");
        ReflectionUtils.makeAccessible(supportedPlatformsField);
        ReflectionUtils.setField(supportedPlatformsField, underTest, "AWS,AZURE");
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
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Stack stack = new Stack();
        stack.setCluster(cluster);

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
        Stack stack = createStack(StackType.DATALAKE, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PRIVATE, loadBalancers.iterator().next().getType());
            assertEquals(1, stack.getInstanceGroups().iterator().next().getTargetGroups().size());
        });
    }

    @Test
    public void testCreateLoadBalancerForDataLakePublicSubnets() {
        Stack stack = createStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
        });
    }

    @Test
    public void testCreateLoadBalancerForDatahubWithDatalakeEntitlement() {
        Stack stack = createStack(StackType.WORKLOAD, PRIVATE_ID_1);
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
        Stack stack = createStack(StackType.DATALAKE, PRIVATE_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancersForEndpointGatewayDatalake() {
        Stack stack = createStack(StackType.DATALAKE, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true);

        when(entitlementService.publicEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));
        });
    }

    @Test
    public void testCreateLoadBalancersForEndpointGatewayDatahub() {
        Stack stack = createStack(StackType.WORKLOAD, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true);

        when(entitlementService.publicEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));
        });
    }

    @Test
    public void testCreateLoadBalancersForEndpointGatewayDatalakePublicSubnetsOnly() {
        Stack stack = createStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true);

        when(entitlementService.publicEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
        });
    }

    @Test
    public void testCreateLoadBalancersUnsupportedStackType() {
        Stack stack1 = createStack(StackType.TEMPLATE, PRIVATE_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack1, environment);
            assert loadBalancers.isEmpty();
        });

        Stack stack2 = createStack(StackType.LEGACY, PRIVATE_ID_1);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack2, environment);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancersNullEnvironment() {
        Stack stack = createStack(StackType.DATALAKE, PRIVATE_ID_1);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, null);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancersEndpointGatewayNullNetwork() {
        Stack stack = createStack(StackType.DATALAKE, PUBLIC_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PUBLIC_ID_1, AZ_1), true);
        environment.setNetwork(null);

        when(entitlementService.publicEndpointAccessGatewayEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerNoStackNetwork() {
        Stack stack = createStack(StackType.DATALAKE, null);
        stack.setNetwork(null);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerNoAttributes() {
        Stack stack = createStack(StackType.DATALAKE, null);
        stack.getNetwork().setAttributes(null);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerNoSubnetSpecified() {
        Stack stack = createStack(StackType.DATALAKE, null);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerNoEnvironmentNetwork() {
        Stack stack = createStack(StackType.DATALAKE, null);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, null);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDN() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PUBLIC_DNS, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNPrivateOnly() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers(true, false);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_DNS, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNPublicMissingDns() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
            .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
            .findFirst().get();
        publicLoadBalancer.setFqdn(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_DNS, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNNoFqdnSet() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
            .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
            .findFirst().get();
        publicLoadBalancer.setFqdn(null);
        LoadBalancer privateLoadBalancer = loadBalancers.stream()
            .filter(lb -> LoadBalancerType.PRIVATE.equals(lb.getType()))
            .findFirst().get();
        privateLoadBalancer.setFqdn(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertNull(fqdn);
    }

    @Test
    public void testCreateLoadBalancerUnsupportedPlatform() {
        Stack stack = createStack(StackType.DATALAKE, PRIVATE_ID_1);
        stack.setCloudPlatform(GCP);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment);
            assertEquals(0, loadBalancers.size());
        });
    }

    @Test
    public void testCreateLoadBalancerDryRun() {
        Stack stack = createStack(StackType.DATALAKE, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            boolean result = underTest.isLoadBalancerCreationConfigured(stack, environment);
            assert result;
            assertEquals("Target groups should not be set on a dry run",
                0, stack.getInstanceGroups().iterator().next().getTargetGroups().size());
        });
    }

    private String getBlueprintText(String path) {
        return FileReaderUtils.readFileFromClasspathQuietly(path);
    }

    private Set<LoadBalancer> createLoadBalancers() {
        return createLoadBalancers(true, true);
    }

    private Set<LoadBalancer> createLoadBalancers(boolean createPrivate, boolean createPublic) {
        Set<LoadBalancer> loadBalancers = new HashSet<>();
        if (createPrivate) {
            LoadBalancer privateLoadBalancer = new LoadBalancer();
            privateLoadBalancer.setType(LoadBalancerType.PRIVATE);
            privateLoadBalancer.setFqdn(PRIVATE_DNS);
            loadBalancers.add(privateLoadBalancer);
        }
        if (createPublic) {
            LoadBalancer publicLoadBalancer = new LoadBalancer();
            publicLoadBalancer.setType(LoadBalancerType.PUBLIC);
            publicLoadBalancer.setFqdn(PUBLIC_DNS);
            loadBalancers.add(publicLoadBalancer);
        }
        return loadBalancers;
    }

    private Stack createStack(StackType type, String subnetId) {
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        Stack stack = new Stack();
        stack.setType(type);
        stack.setCluster(cluster);
        stack.setInstanceGroups(Set.of(instanceGroup));
        stack.setCloudPlatform(AWS);
        Network network = new Network();
        if (StringUtils.isNotEmpty(subnetId)) {
            Map<String, Object> attributes = Map.of("subnetId", subnetId);
            network.setAttributes(new Json(attributes));
        }
        stack.setNetwork(network);
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
