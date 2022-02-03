package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.AwsEnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.authentication.StackAuthenticationV4RequestToStackAuthenticationConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterV4RequestToClusterConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupV4RequestToHostGroupConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupV4RequestToInstanceGroupConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.network.NetworkV4RequestToNetworkConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.GatewaySecurityGroupDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.tag.request.CDPTagMergeRequest;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.TargetGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.TagResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StackV4RequestToStackConverterTest extends AbstractJsonConverterTest<StackV4Request> {

    private static final Map<CloudPlatform, String> TEST_REGIONS = Map.of(
            AWS, "eu-west-2",
            MOCK, "some-mock-region-1");

    private static final String DEFAULT_REGIONS_FIELD_NAME = "defaultRegions";

    @InjectMocks
    private StackV4RequestToStackConverter underTest;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private PreferencesService preferencesService;

    @Mock
    private StackService stackService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private TelemetryConverter telemetryConverter;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private Workspace workspace;

    @Mock
    private User user;

    @Mock
    private Clock clock;

    @Mock
    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap;

    @Mock
    private AwsEnvironmentNetworkConverter awsEnvironmentNetworkConverter;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private GatewaySecurityGroupDecorator gatewaySecurityGroupDecorator;

    @Mock
    private CredentialResponse credentialResponse;

    @Mock
    private CostTagging costTagging;

    @Mock
    private LoadBalancerConfigService loadBalancerConfigService;

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private ClusterV4RequestToClusterConverter clusterV4RequestToClusterConverter;

    @Mock
    private NetworkV4RequestToNetworkConverter networkV4RequestToNetworkConverter;

    @Mock
    private InstanceGroupV4RequestToHostGroupConverter instanceGroupV4RequestToHostGroupConverter;

    @Mock
    private InstanceGroupV4RequestToInstanceGroupConverter instanceGroupV4RequestToInstanceGroupConverter;

    @Mock
    private StackAuthenticationV4RequestToStackAuthenticationConverter stackAuthenticationV4RequestToStackAuthenticationConverter;

    private Credential credential;

    @BeforeEach
    void setUp() {
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(cloudbreakUser.getUsername()).thenReturn("username");
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(1L);
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(1L);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCredential(credentialResponse);
        environmentResponse.setCloudPlatform("AWS");
        environmentResponse.setTunnel(Tunnel.DIRECT);
        environmentResponse.setTags(new TagResponse());
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(costTagging.mergeTags(any(CDPTagMergeRequest.class))).thenReturn(new HashMap<>());
        when(datalakeService.getDatalakeCrn(any(), any())).thenReturn("crn");
        credential = Credential.builder()
                .cloudPlatform("AWS")
                .build();
    }

    @Test
    public void testConvert() {
        initMocks();
        setDefaultRegions(AWS);
        StackV4Request request = getRequest("stack.json");

        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(clusterV4RequestToClusterConverter.convert(any(ClusterV4Request.class))).willReturn(new Cluster());
        given(telemetryConverter.convert(null, StackType.WORKLOAD)).willReturn(new Telemetry());
        // WHEN
        Stack stack = underTest.convert(request);
        // THEN
        assertAllFieldsNotNull(
                stack,
                Arrays.asList("description", "cluster", "environmentCrn", "gatewayPort", "useCcm", "network", "securityConfig",
                        "version", "created", "platformVariant", "cloudPlatform",
                        "customHostname", "customDomain", "clusterNameAsSubdomain", "hostgroupNameAsHostname", "parameters", "creator",
                        "environmentCrn", "terminated", "datalakeCrn", "type", "inputs", "failurePolicy", "resourceCrn", "minaSshdServiceId",
                        "ccmV2AgentCrn", "externalDatabaseCreationType", "stackVersion", "externalDatabaseEngineVersion"));
        assertEquals("eu-west-1", stack.getRegion());
        assertEquals("AWS", stack.getCloudPlatform());
        assertEquals("mystack", stack.getName());
        verify(telemetryConverter, times(1)).convert(null, StackType.WORKLOAD);
        verify(environmentClientService, times(1)).getByCrn(anyString());
        verify(gatewaySecurityGroupDecorator, times(1))
                .extendGatewaySecurityGroupWithDefaultGatewayCidrs(any(Stack.class), any(Tunnel.class));
    }

    @Test
    public void testConvertShouldHaveDefaultTags() {
        initMocks();
        setDefaultRegions(AWS);
        StackV4Request request = getRequest("stack-without-tags.json");

        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(clusterV4RequestToClusterConverter.convert(any(ClusterV4Request.class))).willReturn(new Cluster());
        // WHEN
        Stack stack = underTest.convert(request);
        // THEN
        assertAllFieldsNotNull(
                stack,
                Arrays.asList("description", "cluster", "environmentCrn", "gatewayPort", "useCcm", "network", "securityConfig",
                        "version", "created", "platformVariant", "cloudPlatform", "resourceCrn",
                        "customHostname", "customDomain", "clusterNameAsSubdomain", "hostgroupNameAsHostname", "parameters", "creator",
                        "environmentCrn", "terminated", "datalakeCrn", "type", "inputs", "failurePolicy", "minaSshdServiceId",
                        "ccmV2AgentCrn", "externalDatabaseCreationType", "stackVersion", "externalDatabaseEngineVersion"));
        assertEquals("eu-west-1", stack.getRegion());
        assertEquals("AWS", stack.getCloudPlatform());
        assertEquals("mystack", stack.getName());
    }

    private Mappable getMappable() {
        return new Mappable() {
            @Override
            public Map<String, Object> asMap() {
                return Collections.emptyMap();
            }

            @Override
            public CloudPlatform getCloudPlatform() {
                return null;
            }
        };
    }

    @Test
    public void testConvertWithLoginUserName() {
        initMocks();
        setDefaultRegions(AWS);
        // WHEN
        BadRequestException expectedException = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.convert(getRequest("stack-with-loginusername.json")));

        // THEN
        assertEquals("You can not modify the default user!", expectedException.getMessage());
    }

    @Test
    public void testWhenRegionIsEmptyButDefaultRegionsAreEmptyThenBadRequestExceptionComes() {
        setDefaultRegions(null);
        StackV4Request request = getRequest("stack.json");
        request.setCloudPlatform(MOCK);
        request.getPlacement().setRegion(null);

        BadRequestException resultException = assertThrows(BadRequestException.class, () -> underTest.convert(request));
        assertEquals("No default region is specified. Region cannot be empty.", resultException.getMessage());
    }

    @Test
    void testWhenProvidedRegionIsEmptyButDefaultOnesAreNotAndPlatformRegionIsNullThenBadRequestExceptionComes() {
        setDefaultRegions(AWS);
        StackV4Request request = getRequest("stack.json");
        request.setCloudPlatform(MOCK);
        request.getPlacement().setRegion(null);

        BadRequestException resultException = assertThrows(BadRequestException.class, () -> underTest.convert(request));
        assertEquals(String.format("No default region specified for: %s. Region cannot be empty.",
                request.getCloudPlatform().name()), resultException.getMessage());
    }

    @Test
    void testWhenProvidedRegionIsEmptyAndDefaultOnesAreNotAndPlatformRegionExistsForPlatformThenItShouldBeSet() {
        setDefaultRegions(MOCK);
        StackV4Request request = getRequest("stack.json");
        request.setCloudPlatform(MOCK);
        request.getPlacement().setRegion(null);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);

        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);
        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(instanceGroupV4RequestToInstanceGroupConverter.convert(any(InstanceGroupV4Request.class), anyString())).willReturn(instanceGroup);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(clusterV4RequestToClusterConverter.convert(any(ClusterV4Request.class))).willReturn(new Cluster());

        Stack result = underTest.convert(request);

        assertEquals(TEST_REGIONS.get(MOCK), result.getRegion());
    }

    @Test
    public void testConvertWithKnoxLoadBalancer() {
        initMocks();
        setDefaultRegions(AWS);
        StackV4Request request = getRequest("stack-datalake-with-instancegroups.json");
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        TargetGroup targetGroup = new TargetGroup();
        targetGroup.setType(TargetGroupType.KNOX);
        LoadBalancer loadBalancer = new LoadBalancer();
        loadBalancer.setType(LoadBalancerType.PRIVATE);
        loadBalancer.setTargetGroupSet(Set.of(targetGroup));

        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(clusterV4RequestToClusterConverter.convert(any(ClusterV4Request.class))).willReturn(new Cluster());
        given(telemetryConverter.convert(null, StackType.DATALAKE)).willReturn(new Telemetry());
        given(loadBalancerConfigService.createLoadBalancers(any(), any(), eq(request))).willReturn(Set.of(loadBalancer));
        // WHEN
        Stack stack = underTest.convert(request);
        // THEN
        assertEquals(1, stack.getLoadBalancers().size());
        assertEquals(1, stack.getLoadBalancers().iterator().next().getTargetGroupSet().size());
        assertEquals(TargetGroupType.KNOX, stack.getLoadBalancers().iterator().next().getTargetGroupSet().iterator().next().getType());
    }

    @Test
    public void testNoLoadBalancersCreatedWhenEntitlementIsDisabled() {
        initMocks();
        setDefaultRegions(AWS);
        StackV4Request request = getRequest("stack-datalake-with-instancegroups.json");

        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(clusterV4RequestToClusterConverter.convert(any(ClusterV4Request.class))).willReturn(new Cluster());
        given(telemetryConverter.convert(null, StackType.DATALAKE)).willReturn(new Telemetry());
        given(loadBalancerConfigService.getKnoxGatewayGroups(any(Stack.class))).willReturn(Set.of("master"));
        // WHEN
        Stack stack = underTest.convert(request);
        // THEN
        assertEquals(0, stack.getLoadBalancers().size());
    }

    @Test
    public void testNoEndpointGatewayLoadBalancerWhenEntitlementIsDisabled() {
        StackV4Request request = setupForEndpointGateway(true);
        // WHEN
        Stack stack = underTest.convert(request);
        // THEN
        assertTrue(stack.getLoadBalancers().isEmpty());
    }

    @Test
    public void testNoEndpointGatewayLoadBalancerWhenFlagIsDisabled() {
        StackV4Request request = setupForEndpointGateway(false);
        // WHEN
        Stack stack = underTest.convert(request);
        // THEN
        assertTrue(stack.getLoadBalancers().isEmpty());
    }

    @Override
    public Class<StackV4Request> getRequestClass() {
        return StackV4Request.class;
    }

    private void initMocks() {
        // GIVEN
        InstanceGroup instanceGroup = new InstanceGroup();
        SecurityGroup securityGroup = new SecurityGroup();
        instanceGroup.setSecurityGroup(securityGroup);
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        instanceGroup.setGroupName("master");

        given(stackAuthenticationV4RequestToStackAuthenticationConverter.convert(any(StackAuthenticationV4Request.class))).willReturn(new StackAuthentication());
        given(instanceGroupV4RequestToInstanceGroupConverter.convert(any(InstanceGroupV4Request.class), anyString())).willReturn(instanceGroup);
    }

    private StackV4Request setupForEndpointGateway(boolean enabled) {
        initMocks();
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        StackV4Request request = getRequest("stack-datalake-with-instancegroups.json");

        EnvironmentNetworkResponse networkResponse = new EnvironmentNetworkResponse();
        networkResponse.setPublicEndpointAccessGateway(enabled ? PublicEndpointAccessGateway.ENABLED : PublicEndpointAccessGateway.DISABLED);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCredential(credentialResponse);
        environmentResponse.setCloudPlatform("AWS");
        environmentResponse.setTunnel(Tunnel.DIRECT);
        environmentResponse.setTags(new TagResponse());
        environmentResponse.setNetwork(networkResponse);
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);

        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(clusterV4RequestToClusterConverter.convert(any(ClusterV4Request.class))).willReturn(new Cluster());
        given(telemetryConverter.convert(null, StackType.DATALAKE)).willReturn(new Telemetry());
        given(loadBalancerConfigService.getKnoxGatewayGroups(any(Stack.class))).willReturn(Set.of("master"));

        return request;
    }

    private void setDefaultRegions(CloudPlatform platform) {
        ReflectionTestUtils.setField(underTest, DEFAULT_REGIONS_FIELD_NAME, platform != null ? getTestRegion(platform) : null);
    }

    private String getTestRegion(CloudPlatform platform) {
        String region = TEST_REGIONS.get(platform);
        if (region != null) {
            return platform.name() + ":" + region;
        }
        throw new IllegalArgumentException("No region has found for platform: " + platform);
    }

}
