package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.YARN;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
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
import org.mockito.Spy;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.instance.AvailabilitySetNameService;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceGroupParameters;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerCreation;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

public class LoadBalancerConfigServiceTest extends SubnetTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String PRIVATE_DNS = "private.dns";

    private static final String PRIVATE_FQDN = "private.fqdn";

    private static final String PRIVATE_IP = "private.ip";

    private static final String PUBLIC_FQDN = "public.fqdn";

    private static final String PUBLIC_IP = "public.ip";

    private static final String PUBLIC_DNS = "public.dns";

    @Mock
    private Blueprint blueprint;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private SubnetSelector subnetSelector;

    @Mock
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Mock
    private AvailabilitySetNameService availabilitySetNameService;

    // allows @InjectMocks to provide this component to the LoadBalancerConfigService
    @Spy
    private ProviderParameterCalculator providerParameterCalculator;

    @InjectMocks
    private LoadBalancerConfigService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Field supportedPlatformsField = ReflectionUtils.findField(LoadBalancerConfigService.class, "supportedPlatforms");
        ReflectionUtils.makeAccessible(supportedPlatformsField);
        ReflectionUtils.setField(supportedPlatformsField, underTest, "AWS,AZURE,YARN");
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
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PRIVATE, loadBalancers.iterator().next().getType());
            InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
            assertEquals(1, masterInstanceGroup.getTargetGroups().size());
        });
    }

    @Test
    public void testDisableLoadBalancer() {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        environment.getNetwork().setLoadBalancerCreation(LoadBalancerCreation.DISABLED);
        StackV4Request request = new StackV4Request();

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(0, loadBalancers.size());
        });
    }

    @Test
    public void testCreateLoadBalancerForDataLakePublicSubnets() {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
        });
    }

    @Test
    public void testCreateLoadBalancerForYarn() {
        Stack stack = createYarnStack();
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
        });
    }

    @Test
    public void testCreateLoadBalancerForDatahubWithDatalakeEntitlement() {
        Stack stack = createAwsStack(StackType.WORKLOAD, PRIVATE_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerForDataLakeEntitlementDisabled() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateAWSLoadBalancerForDataLakeEntitlementDisabledPublicSubnets() {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        environment.setCloudPlatform(CloudPlatform.AWS.name());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(false);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
        });
    }

    @Test
    public void testCreateAWSLoadBalancerForDataLakeEntitlementDisabledPrivateSubnets() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        environment.setCloudPlatform(CloudPlatform.AWS.name());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(false);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PRIVATE, loadBalancers.iterator().next().getType());
            InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
            assertEquals(1, masterInstanceGroup.getTargetGroups().size());
        });
    }

    @Test
    public void testCreateLoadBalancersForEndpointGatewayDatalake() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));
        });
    }

    @Test
    public void testCreateLoadBalancersForEndpointGatewayDatahub() {
        Stack stack = createAwsStack(StackType.WORKLOAD, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));
        });
    }

    @Test
    public void testCreateLoadBalancersForDatahubWithPublicSubnet() {
        Stack stack = createAwsStack(StackType.WORKLOAD, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);

        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));
        });
    }

    @Test
    public void testCreateLoadBalancersForDatahubWithPrivateSubnet() {
        Stack stack = createAwsStack(StackType.WORKLOAD, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);

        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
        });
    }

    @Test
    public void testCreateLoadBalancersForEndpointGatewayDatalakePublicSubnetsOnly() {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
        });
    }

    @Test
    public void testCreateLoadBalancersUnsupportedStackType() {
        Stack stack1 = createAwsStack(StackType.TEMPLATE, PRIVATE_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack1, environment, request);
            assert loadBalancers.isEmpty();
        });

        Stack stack2 = createAwsStack(StackType.LEGACY, PRIVATE_ID_1);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack2, environment, request);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancersNullEnvironment() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, null, request);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancersEndpointGatewayNullNetwork() {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PUBLIC_ID_1, AZ_1), true);
        environment.setNetwork(null);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerNoStackNetwork() {
        Stack stack = createAwsStack(StackType.DATALAKE, null);
        stack.setNetwork(null);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerNoAttributes() {
        Stack stack = createAwsStack(StackType.DATALAKE, null);
        stack.getNetwork().setAttributes(null);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerNoSubnetSpecified() {
        Stack stack = createAwsStack(StackType.DATALAKE, null);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testCreateLoadBalancerNoEnvironmentNetwork() {
        Stack stack = createAwsStack(StackType.DATALAKE, null);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, null, request);
            assert loadBalancers.isEmpty();
        });
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDN() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PUBLIC_FQDN, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNNoLBs() {
        Set<LoadBalancer> loadBalancers = Set.of();

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertNull(fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNPrivateOnly() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers(true, false);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_FQDN, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNPublicMissingAll() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
            .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
            .findFirst().get();
        publicLoadBalancer.setFqdn(null);
        publicLoadBalancer.setIp(null);
        publicLoadBalancer.setDns(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_FQDN, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNPublicMissingFQDN() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
                .findFirst().get();
        publicLoadBalancer.setFqdn(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PUBLIC_DNS, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNNoPublicPrivateMissingFQDN() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers(true, false);
        LoadBalancer privateLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PRIVATE.equals(lb.getType()))
                .findFirst().get();
        privateLoadBalancer.setFqdn(null);
        privateLoadBalancer.setIp(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_DNS, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNNoPublicPrivateMissingFQDNNoDNS() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers(true, false);
        LoadBalancer privateLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PRIVATE.equals(lb.getType()))
                .findFirst().get();
        privateLoadBalancer.setFqdn(null);
        privateLoadBalancer.setDns(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PRIVATE_IP, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNIPOnlyMissingFQDN() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
                .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
                .findFirst().get();
        publicLoadBalancer.setFqdn(null);
        publicLoadBalancer.setDns(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertEquals(PUBLIC_IP, fqdn);
    }

    @Test
    public void testGetLoadBalancerUserFacingFQDNNoFqdnSet() {
        Set<LoadBalancer> loadBalancers = createLoadBalancers();
        LoadBalancer publicLoadBalancer = loadBalancers.stream()
            .filter(lb -> LoadBalancerType.PUBLIC.equals(lb.getType()))
            .findFirst().get();
        publicLoadBalancer.setFqdn(null);
        publicLoadBalancer.setIp(null);
        publicLoadBalancer.setDns(null);

        LoadBalancer privateLoadBalancer = loadBalancers.stream()
            .filter(lb -> LoadBalancerType.PRIVATE.equals(lb.getType()))
            .findFirst().get();
        privateLoadBalancer.setFqdn(null);
        privateLoadBalancer.setDns(null);
        privateLoadBalancer.setIp(null);

        when(loadBalancerPersistenceService.findByStackId(0L)).thenReturn(loadBalancers);

        String fqdn = underTest.getLoadBalancerUserFacingFQDN(0L);
        assertNull(fqdn);
    }

    @Test
    public void testCreateLoadBalancerUnsupportedPlatform() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        stack.setCloudPlatform(GCP);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(0, loadBalancers.size());
        });
    }

    @Test
    public void testCreateLoadBalancerDryRun() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
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

    @Test
    public void testCreateAzurePrivateLoadBalancer() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType()));
            InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
            assertEquals(1, masterInstanceGroup.getTargetGroups().size());

            checkAvailabilitySetAttributes(loadBalancers);
        });
    }

    @Test
    public void testCreateAzurePublicLoadBalancer() {
        Stack stack = createAzureStack(StackType.DATALAKE, PUBLIC_ID_1, false);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
            InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
            assertEquals(1, masterInstanceGroup.getTargetGroups().size());

            checkAvailabilitySetAttributes(loadBalancers);
        });
    }

    @Test
    public void testCreateAzurePrivateLoadBalancerWithOozieHA() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/de-ha.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType()));
            InstanceGroup managerInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "manager".equals(ig.getGroupName()))
                .findFirst().get();
            assertEquals(2, managerInstanceGroup.getTargetGroups().size());

            checkAvailabilitySetAttributes(loadBalancers);
        });
    }

    @Test
    public void testAzureLoadBalancerDisabledWithOozieHA() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.NONE);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);
        request.setAzure(azureParameters);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/de-ha.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.createLoadBalancers(stack, environment, request));
    }

    @Test
    public void testCreateAzureEndpointGateway() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));

            checkAvailabilitySetAttributes(loadBalancers);
        });
    }

    @Test
    public void testCreateAzureLoadBalancerWithSkuSetToStandard() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.STANDARD);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);
        request.setAzure(azureParameters);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(2, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType()));
            InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                    .filter(ig -> "master".equals(ig.getGroupName()))
                    .findFirst().get();
            assertEquals(1, masterInstanceGroup.getTargetGroups().size());
            assertTrue(loadBalancers.stream().allMatch(l -> LoadBalancerSku.STANDARD.equals(l.getSku())));

            checkAvailabilitySetAttributes(loadBalancers);
        });
    }

    @Test
    public void testCreateAzureLoadBalancerWithSkuSetToBasic() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.BASIC);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);
        request.setAzure(azureParameters);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assertEquals(1, loadBalancers.size());
            assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                    .filter(ig -> "master".equals(ig.getGroupName()))
                    .findFirst().get();
            assertEquals(1, masterInstanceGroup.getTargetGroups().size());
            assertTrue(loadBalancers.stream().allMatch(l -> LoadBalancerSku.BASIC.equals(l.getSku())));

            checkAvailabilitySetAttributes(loadBalancers);
        });
    }

    @Test
    public void testCreateAzureLoadBalancerSelectsDefaultWhenSkuIsNotSet() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            if (LoadBalancerSku.getDefault().equals(LoadBalancerSku.STANDARD)) {
                assertEquals(2, loadBalancers.size());
                assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
                assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType()));
            } else if (LoadBalancerSku.getDefault().equals(LoadBalancerSku.BASIC)) {
                assertEquals(1, loadBalancers.size());
                assert loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            }
            InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                    .filter(ig -> "master".equals(ig.getGroupName()))
                    .findFirst().get();
            assertEquals(1, masterInstanceGroup.getTargetGroups().size());
            assertTrue(loadBalancers.stream().allMatch(l -> LoadBalancerSku.getDefault().equals(l.getSku())));

            checkAvailabilitySetAttributes(loadBalancers);
        });
    }

    @Test
    public void testAzureLoadBalancerDisabled() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.NONE);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);
        request.setAzure(azureParameters);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
            assert loadBalancers.isEmpty();
        });
    }

    /**
     * Verifies that all instance groups routed to by a set of load balancers include an availability set.
     *
     * For an Azure load balancer using the basic SKU to route to a number of instances,
     * they must be in the same availability set.
     *
     * We group related Azure virtual machine instances into the same instance group,
     * so we ensure each of them has an availability set attribute.
     *
     * This check is only necessary as long as we're using availability sets in instance groups,
     * which is itself only necessary while using the basic, rather than standard, SKU.
     * @param loadBalancers the set of load balancers to check for a related availability set attribute.
     */
    private void checkAvailabilitySetAttributes(Set<LoadBalancer> loadBalancers) {
        // we traverse lbs -> target groups -> instance groups -> instance group attributes
        Set<Map<String, Object>> allAttributes = loadBalancers.stream()
                .flatMap(lb -> lb.getTargetGroupSet().stream())
                .flatMap(targetGroup -> targetGroup.getInstanceGroups().stream())
                .map(InstanceGroup::getAttributes)
                .map(Json::getMap)
                .collect(toSet());

        assertTrue(allAttributes.stream().allMatch(set -> set.containsKey("availabilitySet")));

        boolean availabilitySetAttributeContainsExpectedKeys = allAttributes.stream().allMatch(set -> {
            Map<String, String> attributes = (Map<String, String>) set.get("availabilitySet");
            return attributes.containsKey(AzureInstanceGroupParameters.NAME) &&
                    attributes.containsKey(AzureInstanceGroupParameters.FAULT_DOMAIN_COUNT) &&
                    attributes.containsKey(AzureInstanceGroupParameters.UPDATE_DOMAIN_COUNT);
        });
        assertTrue(availabilitySetAttributeContainsExpectedKeys);
    }

    @Test
    public void testCreateAzureMultipleKnoxInstanceGroups() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(entitlementService.datalakeLoadBalancerEnabled(anyString())).thenReturn(true);
        when(blueprint.getBlueprintText()).thenReturn(getBlueprintText("input/clouderamanager-multi-knox.bp"));
        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.createLoadBalancers(stack, environment, request);
                fail("Should not allow multiple knox groups with Azure load balancers");
            } catch (CloudbreakServiceException ignored) {
                // pass
            }
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
            privateLoadBalancer.setFqdn(PRIVATE_FQDN);
            privateLoadBalancer.setDns(PRIVATE_DNS);
            privateLoadBalancer.setIp(PRIVATE_IP);
            loadBalancers.add(privateLoadBalancer);
        }
        if (createPublic) {
            LoadBalancer publicLoadBalancer = new LoadBalancer();
            publicLoadBalancer.setType(LoadBalancerType.PUBLIC);
            publicLoadBalancer.setFqdn(PUBLIC_FQDN);
            publicLoadBalancer.setDns(PUBLIC_DNS);
            publicLoadBalancer.setIp(PUBLIC_IP);
            loadBalancers.add(publicLoadBalancer);
        }
        return loadBalancers;
    }

    private Stack createAwsStack(StackType type, String subnetId) {
        return createStack(type, subnetId, AWS, false);
    }

    private Stack createAzureStack(StackType type, String subnetId, boolean makePrivate) {
        return createStack(type, subnetId, AZURE, makePrivate);
    }

    private Stack createStack(StackType type, String subnetId, String cloudPlatform, boolean makePrivate) {
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        InstanceGroup instanceGroup1 = new InstanceGroup();
        instanceGroup1.setGroupName("master");
        instanceGroup1.setAttributes(new Json(new HashMap<String, Object>()));
        InstanceGroup instanceGroup2 = new InstanceGroup();
        instanceGroup2.setGroupName("manager");
        instanceGroup2.setAttributes(new Json(new HashMap<String, Object>()));
        instanceGroups.add(instanceGroup1);
        instanceGroups.add(instanceGroup2);
        InstanceMetaData imd1 = new InstanceMetaData();
        InstanceMetaData imd2 = new InstanceMetaData();
        Set<InstanceMetaData> imdSet = Set.of(imd1, imd2);
        instanceGroup1.setInstanceMetaData(imdSet);
        instanceGroup2.setInstanceMetaData(imdSet);
        Stack stack = new Stack();
        stack.setType(type);
        stack.setCluster(cluster);
        stack.setInstanceGroups(instanceGroups);
        stack.setCloudPlatform(cloudPlatform);
        Network network = new Network();
        Map<String, Object> attributes = new HashMap<>();
        if (StringUtils.isNotEmpty(subnetId)) {
            attributes.put("subnetId", subnetId);
        }
        if (AZURE.equals(cloudPlatform)) {
            attributes.put("noPublicIp", makePrivate);
            InstanceGroup instanceGroup3 = new InstanceGroup();
            instanceGroup3.setGroupName("worker");
            instanceGroups.add(instanceGroup3);
        }
        network.setAttributes(new Json(attributes));
        stack.setNetwork(network);
        return stack;
    }

    private Stack createYarnStack() {
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(cluster);
        stack.setInstanceGroups(Set.of(instanceGroup));
        stack.setCloudPlatform(YARN);
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
