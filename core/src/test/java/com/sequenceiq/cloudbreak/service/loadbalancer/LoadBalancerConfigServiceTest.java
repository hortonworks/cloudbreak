package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AZURE;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.GCP;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.YARN;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE;
import static org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.instance.AvailabilitySetNameService;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceGroupParameters;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.SubnetSelector;
import com.sequenceiq.cloudbreak.core.network.SubnetTest;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerCreation;
import com.sequenceiq.common.api.type.LoadBalancerSku;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;

@ExtendWith(MockitoExtension.class)
class LoadBalancerConfigServiceTest extends SubnetTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String PRIVATE_DNS = "private.dns";

    private static final String PRIVATE_FQDN = "private.fqdn";

    private static final String PRIVATE_IP = "private.ip";

    private static final String PUBLIC_FQDN = "public.fqdn";

    private static final String PUBLIC_IP = "public.ip";

    private static final String PUBLIC_DNS = "public.dns";

    @Mock
    private SubnetSelector subnetSelector;

    // allows @InjectMocks to provide this component to the LoadBalancerConfigService
    @Spy
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private AvailabilitySetNameService availabilitySetNameService;

    @Mock
    private LoadBalancerEnabler loadBalancerEnabler;

    @Mock
    private KnoxGroupDeterminer knoxGroupDeterminer;

    @Mock
    private OozieTargetGroupProvisioner oozieTargetGroupProvisioner;

    @Mock
    private LoadBalancerTypeDeterminer loadBalancerTypeDeterminer;

    @InjectMocks
    private LoadBalancerConfigService underTest;

    @RepeatedTest(10)
    void testSelectPrivateLoadBalancer() {
        // Repeat test to ensure we're really choosing the private load balancer every time, and not just
        // coincidentally choosing it because it shows up first in the set.
        Optional<LoadBalancer> loadBalancer = underTest.selectLoadBalancerForFrontend(createLoadBalancers(), LoadBalancerType.PRIVATE);
        assertThat(loadBalancer).isPresent();
        assertThat(loadBalancer.get().getType()).isEqualTo(LoadBalancerType.PRIVATE);
    }

    @RepeatedTest(10)
    void testSelectPublicLoadBalancer() {
        // Repeat test to ensure we're really choosing the private load balancer every time, and not just
        // coincidentally choosing it because it shows up first in the set.
        Optional<LoadBalancer> loadBalancer = underTest.selectLoadBalancerForFrontend(createLoadBalancers(), LoadBalancerType.PUBLIC);
        assertThat(loadBalancer).isPresent();
        assertThat(loadBalancer.get().getType()).isEqualTo(LoadBalancerType.PUBLIC);
    }

    @RepeatedTest(100)
    void testNeverSelectOutboundLoadBalancer() {
        // Repeat test to ensure we're really choosing the private load balancer every time, and not just
        // coincidentally choosing it because it shows up first in the set.
        // Note that we recreate the load balancer every loop to ensure the order is random. If the order
        // isn't random this test can erroneously pass.
        Optional<LoadBalancer> loadBalancer = underTest.selectLoadBalancerForFrontend(createLoadBalancersWithOutbound(), LoadBalancerType.PUBLIC);
        assertThat(loadBalancer).isPresent();
        assertThat(loadBalancer.get().getType()).isEqualTo(LoadBalancerType.PUBLIC);
        loadBalancer = underTest.selectLoadBalancerForFrontend(createLoadBalancersWithOutboundNoPublic(), LoadBalancerType.PUBLIC);
        assertThat(loadBalancer).isPresent();
        assertThat(loadBalancer.get().getType()).isEqualTo(LoadBalancerType.PRIVATE);
    }

    @Test
    void testCreateLoadBalancerForDataLakePrivateSubnets() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertEquals(LoadBalancerType.PRIVATE, loadBalancers.iterator().next().getType());
        InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, masterInstanceGroup.getTargetGroups().size());
    }

    @Test
    void testDisableLoadBalancer() {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "AWS", true, Optional.empty());
        environment.getNetwork().setLoadBalancerCreation(LoadBalancerCreation.DISABLED);
        StackV4Request request = new StackV4Request();

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(0, loadBalancers.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testCreateLoadBalancerForDataLakePublicSubnets(boolean enablePublicEndpointGateway) {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        CloudSubnet peagSubnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_2);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, enablePublicEndpointGateway, "AWS", true,
                Optional.of(peagSubnet));
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(
                enablePublicEndpointGateway ? LoadBalancerType.PUBLIC : LoadBalancerType.GATEWAY_PRIVATE);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertEquals(enablePublicEndpointGateway ? LoadBalancerType.PUBLIC : LoadBalancerType.GATEWAY_PRIVATE, loadBalancers.iterator().next().getType());
    }

    @Test
    void testCreateLoadBalancerForYarn() {
        Stack stack = createYarnStack();
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true, "YARN", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
    }

    @Test
    void testCreateLoadBalancerForDatahubPrivateSubnet() {
        Stack stack = createAwsStack(StackType.WORKLOAD, PRIVATE_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), false, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(false);
        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertThat(loadBalancers).isEmpty();
    }

    @Test
    void testCreateLoadBalancerForDataLakeEntitlementDisabled() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        DetailedEnvironmentResponse environment = createAzureEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), false, false, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertThat(loadBalancers).isEmpty();
    }

    @Test
    void testCreateAWSLoadBalancerForDataLakeEntitlementDisabledPublicSubnets() {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true, "AWS", true, Optional.empty());
        environment.setCloudPlatform(CloudPlatform.AWS.name());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
    }

    @Test
    void testCreateAWSLoadBalancerForDataLakeEntitlementDisabledPrivateSubnets() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "AWS", true, Optional.empty());
        environment.setCloudPlatform(CloudPlatform.AWS.name());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertEquals(LoadBalancerType.PRIVATE, loadBalancers.iterator().next().getType());
        InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, masterInstanceGroup.getTargetGroups().size());
    }

    @ParameterizedTest
    @EnumSource(value = StackType.class, names = {"DATALAKE", "WORKLOAD"}, mode = INCLUDE)
    void testCreateLoadBalancersForEndpointGateway(StackType stackType) {
        Stack stack = createAwsStack(stackType, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(2, loadBalancers.size());
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));
    }

    @Test
    void testCreateLoadBalancersForDatahubWithPublicSubnet() {
        Stack stack = createAwsStack(StackType.WORKLOAD, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));
    }

    @Test
    void testCreateLoadBalancersForDatahubWithPrivateSubnet() {
        Stack stack = createAwsStack(StackType.WORKLOAD, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
    }

    @Test
    void testCreateLoadBalancersForEndpointGatewayDatalakePublicSubnetsOnly() {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, true, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
    }

    @Test
    void testCreateLoadBalancersUnsupportedStackType() {
        Stack stack1 = createAwsStack(StackType.TEMPLATE, PRIVATE_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1), true, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack1, environment, request);
        assertThat(loadBalancers).isEmpty();

        Stack stack2 = createAwsStack(StackType.LEGACY, PRIVATE_ID_1);

        Set<LoadBalancer> loadBalancers2 = underTest.createLoadBalancers(stack2, environment, request);
        assertThat(loadBalancers2).isEmpty();
    }

    @Test
    void testCreateLoadBalancersNullEnvironment() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, null, request);
        assertThat(loadBalancers).isEmpty();
    }

    @Test
    void testCreateLoadBalancersEndpointGatewayNullNetwork() {
        Stack stack = createAwsStack(StackType.DATALAKE, PUBLIC_ID_1);
        DetailedEnvironmentResponse environment = createEnvironment(getPrivateCloudSubnet(PUBLIC_ID_1, AZ_1), true, "AWS", true, Optional.empty());
        environment.setNetwork(null);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertThat(loadBalancers).isEmpty();
    }

    @Test
    void testCreateLoadBalancerNoStackNetwork() {
        Stack stack = createAwsStack(StackType.DATALAKE, null);
        stack.setNetwork(null);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertThat(loadBalancers).isEmpty();
    }

    @Test
    void testCreateLoadBalancerNoAttributes() {
        Stack stack = createAwsStack(StackType.DATALAKE, null);
        stack.getNetwork().setAttributes(null);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertThat(loadBalancers).isEmpty();
    }

    @Test
    void testCreateLoadBalancerNoSubnetSpecified() {
        Stack stack = createAwsStack(StackType.DATALAKE, null);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "AWS", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertThat(loadBalancers).isEmpty();
    }

    @Test
    void testCreateLoadBalancerNoEnvironmentNetwork() {
        Stack stack = createAwsStack(StackType.DATALAKE, null);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, null, request);
        assertThat(loadBalancers).isEmpty();
    }

    @Test
    void testCreateLoadBalancerUnsupportedPlatform() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        stack.setCloudPlatform(GCP);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "GCP", true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(0, loadBalancers.size());
    }

    @Test
    void testCreateLoadBalancerDryRun() {
        Stack stack = createAwsStack(StackType.DATALAKE, PRIVATE_ID_1);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, "AWS", true, Optional.empty());

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);

        boolean result = underTest.isLoadBalancerCreationConfigured(stack, environment);
        assertTrue(result);
        assertEquals(0, stack.getInstanceGroups().iterator().next().getTargetGroups().size(), "Target groups should not be set on a dry run");
    }

    @Test
    void testCreateAzurePrivateLoadBalancer() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, true, false, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);

        assertEquals(2, loadBalancers.size());
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType()));
        InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, masterInstanceGroup.getTargetGroups().size());

        checkAvailabilitySetAttributes(stack, loadBalancers);
    }

    @Test
    void testCreateAzurePublicLoadBalancer() {
        Stack stack = createAzureStack(StackType.DATALAKE, PUBLIC_ID_1, false);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, true, false, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertEquals(LoadBalancerType.PUBLIC, loadBalancers.iterator().next().getType());
        InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, masterInstanceGroup.getTargetGroups().size());

        checkAvailabilitySetAttributes(stack, loadBalancers);
    }

    @Test
    void testCreateAzurePrivateLoadBalancerWithOozieHA() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, false, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("manager"));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(false);
        when(oozieTargetGroupProvisioner.setupOozieHATargetGroup(stack, false)).thenReturn(Optional.of(new TargetGroup()));

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(2, loadBalancers.size());
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType()));
        InstanceGroup managerInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "manager".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, managerInstanceGroup.getTargetGroups().size());

        checkAvailabilitySetAttributes(stack, loadBalancers);
    }

    @Test
    void testAzureLoadBalancerDisabledWithOozieHA() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, false, true);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.NONE);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);
        request.setAzure(azureParameters);

        when(oozieTargetGroupProvisioner.setupOozieHATargetGroup(stack, true)).thenReturn(Optional.of(new TargetGroup()));

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.createLoadBalancers(stack, environment, request));
    }

    @Test
    void testCreateAzureEndpointGateway() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, true, false, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(2, loadBalancers.size());
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PUBLIC.equals(l.getType()));

        checkAvailabilitySetAttributes(stack, loadBalancers);
    }

    @Test
    void testCreateAzureLoadBalancerWithSkuSetToStandard() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, false, true);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.STANDARD);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);
        request.setAzure(azureParameters);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(2, loadBalancers.size());
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType()));
        InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, masterInstanceGroup.getTargetGroups().size());
        assertTrue(loadBalancers.stream().allMatch(l -> LoadBalancerSku.STANDARD.equals(l.getSku())));

        checkAvailabilitySetAttributes(stack, loadBalancers);
    }

    @Test
    void testCreateAzureLoadBalancerWithSkuSetToBasic() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, false, true);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.BASIC);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);
        request.setAzure(azureParameters);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertEquals(1, loadBalancers.size());
        assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
        InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, masterInstanceGroup.getTargetGroups().size());
        assertTrue(loadBalancers.stream().allMatch(l -> LoadBalancerSku.BASIC.equals(l.getSku())));

        checkAvailabilitySetAttributes(stack, loadBalancers);
    }

    @Test
    void testCreateAzureLoadBalancerSelectsDefaultWhenSkuIsNotSet() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, false, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        if (LoadBalancerSku.getDefault().equals(LoadBalancerSku.STANDARD)) {
            assertEquals(2, loadBalancers.size());
            assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
            assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType()));
        } else if (LoadBalancerSku.getDefault().equals(LoadBalancerSku.BASIC)) {
            assertEquals(1, loadBalancers.size());
            assertThat(loadBalancers).anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType()));
        }
        InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, masterInstanceGroup.getTargetGroups().size());
        assertTrue(loadBalancers.stream().allMatch(l -> LoadBalancerSku.getDefault().equals(l.getSku())));

        checkAvailabilitySetAttributes(stack, loadBalancers);
    }

    @Test
    void testAzureLoadBalancerDisabled() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, false, true);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.NONE);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);
        request.setAzure(azureParameters);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);
        assertThat(loadBalancers).isEmpty();
    }

    /**
     * Verifies that all instance groups routed to by a set of load balancers include an availability set.
     * <p>
     * For an Azure load balancer using the basic SKU to route to a number of instances,
     * they must be in the same availability set.
     * <p>
     * We group related Azure virtual machine instances into the same instance group,
     * so we ensure each of them has an availability set attribute.
     * <p>
     * This check is only necessary as long as we're using availability sets in instance groups,
     * which is itself only necessary while using the basic, rather than standard, SKU.
     *
     * @param stack stack object
     */
    private void checkAvailabilitySetAttributes(Stack stack, Set<LoadBalancer> loadBalancers) {
        // we traverse lbs -> target groups -> instance groups -> instance group attributes
        Set<Map<String, Object>> allAttributes = loadBalancers.stream()
                .flatMap(lb -> lb.getTargetGroupSet().stream())
                .flatMap(targetGroup -> stack.getInstanceGroups().stream()
                        .filter(instanceGroup -> instanceGroup.getTargetGroups().contains(targetGroup)))
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
    void testCreateAzureMultipleKnoxInstanceGroups() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, false, true);
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(false);

        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master", "worker"));
        assertThatThrownBy(() -> underTest.createLoadBalancers(stack, environment, request))
                .withFailMessage("Should not allow multiple knox groups with Azure load balancers")
                .isInstanceOf(CloudbreakServiceException.class);
    }

    @Test
    void isDatalakeLoadBalancerEntitlementEnabled() {
        Stack azureStack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, false, true);
        Set<LoadBalancer> loadBalancers = underTest.setupLoadBalancers(azureStack, environment, false, true, LoadBalancerSku.BASIC);
        assertEquals(new HashSet<>(), loadBalancers);
    }

    @Test
    void testCreateAzureLoadBalancerWithoutOutboundLoadBalancer() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, true, true);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.STANDARD);
        StackV4Request request = new StackV4Request();
        request.setAzure(azureParameters);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);

        assertEquals(1, loadBalancers.size());
        assertTrue(loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType())));
        assertTrue(loadBalancers.stream().noneMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType())));
        InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, masterInstanceGroup.getTargetGroups().size());
        assertTrue(loadBalancers.stream().allMatch(l -> LoadBalancerSku.STANDARD.equals(l.getSku())));

        checkAvailabilitySetAttributes(stack, loadBalancers);
    }

    @Test
    void testWhenNewNetworkIsCreatedShouldEnableOutboundLoadBalancerByDefault() {
        Stack stack = createAzureStack(StackType.DATALAKE, PRIVATE_ID_1, true);
        CloudSubnet subnet = getPrivateCloudSubnet(PRIVATE_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createAzureEnvironment(subnet, false, null, false);
        AzureStackV4Parameters azureParameters = new AzureStackV4Parameters();
        azureParameters.setLoadBalancerSku(LoadBalancerSku.STANDARD);
        StackV4Request request = new StackV4Request();
        request.setAzure(azureParameters);

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(availabilitySetNameService.generateName(any(), any())).thenReturn("");
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(false);

        Set<LoadBalancer> loadBalancers = underTest.createLoadBalancers(stack, environment, request);

        assertEquals(2, loadBalancers.size());
        assertTrue(loadBalancers.stream().allMatch(l -> LoadBalancerSku.STANDARD.equals(l.getSku())));
        assertTrue(loadBalancers.stream().anyMatch(l -> LoadBalancerType.PRIVATE.equals(l.getType())));
        assertTrue(loadBalancers.stream().anyMatch(l -> LoadBalancerType.OUTBOUND.equals(l.getType())));
        InstanceGroup masterInstanceGroup = stack.getInstanceGroups().stream()
                .filter(ig -> "master".equals(ig.getGroupName()))
                .findFirst().get();
        assertEquals(1, masterInstanceGroup.getTargetGroups().size());
        checkAvailabilitySetAttributes(stack, loadBalancers);
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS"}, mode = EXCLUDE)
    @DisplayName("When the cloud provider is not AWS but network is filled then the sticky session load balancer target should be set to false")
    void testStickySessionLBTargetNotAws(CloudPlatform cloudPlatform) {
        Stack stack = createStack(StackType.WORKLOAD, PUBLIC_ID_1, cloudPlatform.name(), false);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, cloudPlatform.name(), true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);
        request.setNetwork(new NetworkV4Request());

        when(subnetSelector.findSubnetById(any(), anyString())).thenReturn(Optional.of(subnet));
        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);
        lenient().when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        lenient().when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        underTest.createLoadBalancers(stack, environment, request)
                .forEach(lb -> lb.getTargetGroupSet()
                        .forEach(tg -> assertFalse(tg.isUseStickySession())));
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    @DisplayName("When network parameter is not set in Stack then the sticky session load balancer target should be set to false")
    void testStickySessionLBTargetNetworkNotSet(CloudPlatform cloudPlatform) {
        Stack stack = createStack(StackType.WORKLOAD, PUBLIC_ID_1, cloudPlatform.name(), false);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, cloudPlatform.name(), true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);
        stack.setNetwork(null);

        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);
        lenient().when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        lenient().when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        underTest.createLoadBalancers(stack, environment, request)
                .forEach(lb -> lb.getTargetGroupSet()
                        .forEach(tg -> assertFalse(tg.isUseStickySession())));
    }

    @Test
    @DisplayName("When the cloud provider is AWS and network is given in request but not loadBalancer is present in the network Attributes, then the sticky " +
            "session load balancer target should be set to false")
    void testStickySessionLBTargetLoadBalancerAttrNotGiven() {
        Stack stack = createStack(StackType.WORKLOAD, PUBLIC_ID_1, AWS, false);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, AWS, true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);
        stack.getNetwork().setAttributes(new Json("{}"));

        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);
        lenient().when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        lenient().when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        underTest.createLoadBalancers(stack, environment, request)
                .forEach(lb -> lb.getTargetGroupSet()
                        .forEach(tg -> assertFalse(tg.isUseStickySession())));
    }

    @Test
    @DisplayName("When the cloud provider is AWS and network is given along with the loadBalancer attribute but no stickySessionForLoadBalancerTarget set, t" +
            "hen the sticky session load balancer target should not be set")
    void testStickySessionLBTargetAwsLoadBalancerEmpty() {
        Stack stack = createStack(StackType.WORKLOAD, PUBLIC_ID_1, AWS, false);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, AWS, true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);
        stack.getNetwork().setAttributes(new Json("{\"loadBalancer\": {}}"));

        lenient().when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        lenient().when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        underTest.createLoadBalancers(stack, environment, request)
                .forEach(lb -> lb.getTargetGroupSet()
                        .forEach(tg -> assertNull(tg.isUseStickySession())));
    }

    @Test
    @DisplayName("When the cloud provider is AWS and network is given along with the loadBalancer attribute but stickySessionForLoadBalancerTarget set to " +
            "null, then the sticky session load balancer target should be false")
    void testStickySessionLBTargetStickySessNull() {
        Stack stack = createStack(StackType.WORKLOAD, PUBLIC_ID_1, AWS, false);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, AWS, true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);
        stack.getNetwork().setAttributes(new Json("{\"loadBalancer\": {\"stickySessionForLoadBalancerTarget\": null}}"));

        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);
        lenient().when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        lenient().when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        underTest.createLoadBalancers(stack, environment, request)
                .forEach(lb -> lb.getTargetGroupSet()
                        .forEach(tg -> assertFalse(tg.isUseStickySession())));
    }

    @Test
    @DisplayName("When the cloud provider is AWS and network is given along with the loadBalancer attribute but stickySessionForLoadBalancerTarget set " +
            "to false, then the sticky session load balancer target should be false")
    void testStickySessionLBTargetStickySessFalse() {
        Stack stack = createStack(StackType.WORKLOAD, PUBLIC_ID_1, AWS, false);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, AWS, true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);
        stack.getNetwork().setAttributes(new Json("{\"loadBalancer\": {\"stickySessionForLoadBalancerTarget\": false}}"));

        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);
        lenient().when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        lenient().when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        underTest.createLoadBalancers(stack, environment, request)
                .forEach(lb -> lb.getTargetGroupSet()
                        .forEach(tg -> assertFalse(tg.isUseStickySession())));
    }

    @Test
    @DisplayName("When the cloud provider is AWS and network is given along with the loadBalancer attribute but stickySessionForLoadBalancerTarget set " +
            "to true, then the sticky session load balancer target should be true")
    void testStickySessionLBTargetStickySessTrue() {
        Stack stack = createStack(StackType.WORKLOAD, PUBLIC_ID_1, AWS, false);
        CloudSubnet subnet = getPublicCloudSubnet(PUBLIC_ID_1, AZ_1);
        DetailedEnvironmentResponse environment = createEnvironment(subnet, false, AWS, true, Optional.empty());
        StackV4Request request = new StackV4Request();
        request.setEnableLoadBalancer(true);
        stack.getNetwork().setAttributes(new Json("{\"loadBalancer\": {\"stickySessionForLoadBalancerTarget\": true}}"));

        when(loadBalancerEnabler.isLoadBalancerEnabled(any(), any(), any(), anyBoolean())).thenReturn(true);
        when(knoxGroupDeterminer.getKnoxGatewayGroupNames(stack)).thenReturn(Set.of("master"));
        when(loadBalancerTypeDeterminer.getType(environment)).thenReturn(LoadBalancerType.PUBLIC);
        lenient().when(loadBalancerEnabler.isEndpointGatewayEnabled(ACCOUNT_ID, environment.getNetwork())).thenReturn(true);
        lenient().when(availabilitySetNameService.generateName(any(), any())).thenReturn("");

        underTest.createLoadBalancers(stack, environment, request)
                .forEach(lb -> lb.getTargetGroupSet()
                        .forEach(tg -> assertTrue(tg.isUseStickySession())));
    }

    private Set<LoadBalancer> createLoadBalancers() {
        return createLoadBalancers(true, true, false);
    }

    private Set<LoadBalancer> createLoadBalancersWithOutbound() {
        return createLoadBalancers(true, true, true);
    }

    private Set<LoadBalancer> createLoadBalancersWithOutboundNoPublic() {
        return createLoadBalancers(true, false, true);
    }

    private Set<LoadBalancer> createLoadBalancers(boolean createPrivate, boolean createPublic, boolean createOutbound) {
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
        if (createOutbound) {
            LoadBalancer outboundLoadBalancer = new LoadBalancer();
            outboundLoadBalancer.setType(LoadBalancerType.OUTBOUND);
            loadBalancers.add(outboundLoadBalancer);
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
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        InstanceGroup instanceGroup1 = new InstanceGroup();
        instanceGroup1.setGroupName("master");
        instanceGroup1.setInstanceGroupType(InstanceGroupType.GATEWAY);
        instanceGroup1.setAttributes(new Json(new HashMap<String, Object>()));
        InstanceGroup instanceGroup2 = new InstanceGroup();
        instanceGroup2.setGroupName("manager");
        instanceGroup2.setInstanceGroupType(InstanceGroupType.GATEWAY);
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
        Workspace workspace = new Workspace();
        workspace.setName("tenant");
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);
        Map<String, Object> attributes = new HashMap<>();
        if (StringUtils.isNotEmpty(subnetId)) {
            attributes.put("subnetId", subnetId);
        }
        if (AZURE.equals(cloudPlatform)) {
            attributes.put("noPublicIp", makePrivate);
            InstanceGroup instanceGroup3 = new InstanceGroup();
            instanceGroup3.setGroupName("worker");
            instanceGroup3.setInstanceGroupType(InstanceGroupType.GATEWAY);
            instanceGroups.add(instanceGroup3);
        }
        network.setAttributes(new Json(attributes));
        stack.setNetwork(network);
        return stack;
    }

    private Stack createYarnStack() {
        Cluster cluster = new Cluster();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        stack.setCluster(cluster);
        stack.setInstanceGroups(Set.of(instanceGroup));
        stack.setCloudPlatform(YARN);
        Workspace workspace = new Workspace();
        workspace.setName("tenant");
        Tenant tenant = new Tenant();
        tenant.setName("tenant");
        workspace.setTenant(tenant);
        stack.setWorkspace(workspace);
        return stack;
    }

    private DetailedEnvironmentResponse createEnvironment(CloudSubnet subnet, boolean enableEndpointGateway, String cloudPlatform, boolean existingNetwork,
            Optional<CloudSubnet> endpointGatwaySubnet) {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setSubnetMetas(Map.of("key", subnet));
        network.setExistingNetwork(existingNetwork);
        if (enableEndpointGateway) {
            network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        }
        endpointGatwaySubnet.ifPresent(sn -> network.setGatewayEndpointSubnetMetas(Map.of("eagSn", sn)));
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setNetwork(network);
        environment.setCloudPlatform(cloudPlatform);
        environment.setAccountId(ACCOUNT_ID);
        return environment;
    }

    private DetailedEnvironmentResponse createAzureEnvironment(CloudSubnet subnet, boolean enableEndpointGateway, Boolean noOutboundLoadBalancer,
            boolean existingNetwork) {
        DetailedEnvironmentResponse environment = createEnvironment(subnet, enableEndpointGateway, AZURE, existingNetwork, Optional.empty());
        EnvironmentNetworkAzureParams azureParameters = EnvironmentNetworkAzureParams.EnvironmentNetworkAzureParamsBuilder
                .anEnvironmentNetworkAzureParams()
                .withNoOutboundLoadBalancer(noOutboundLoadBalancer)
                .build();
        environment.getNetwork().setAzure(azureParameters);
        return environment;
    }
}
