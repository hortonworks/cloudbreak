package com.sequenceiq.cloudbreak.converter.v4.stacks;

import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_CREATION_TIMESTAMP;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_USER_NAME;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.CB_VERSION;
import static com.sequenceiq.cloudbreak.common.type.DefaultApplicationTag.OWNER;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.authentication.StackAuthenticationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.mappable.Mappable;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.DefaultCostTaggingService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.AbstractJsonConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.AwsEnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.converter.v4.environment.network.EnvironmentNetworkConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.freeipa.FreeIpaAdapter;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.account.PreferencesService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.GatewaySecurityGroupDecorator;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public class StackV4RequestToStackConverterTest extends AbstractJsonConverterTest<StackV4Request> {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private StackV4RequestToStackConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private PreferencesService preferencesService;

    @Mock
    private DefaultCostTaggingService defaultCostTaggingService;

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
    private DatalakeResourcesService datalakeResourcesService;

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
    private FreeIpaAdapter freeIPAAdapter;

    @Mock
    private GatewaySecurityGroupDecorator gatewaySecurityGroupDecorator;

    @Mock
    private CredentialResponse credentialResponse;

    private Credential credential;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(cloudbreakUser.getUsername()).thenReturn("username");
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(1L);
        when(workspaceService.getForCurrentUser()).thenReturn(workspace);
        when(workspace.getId()).thenReturn(1L);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCredential(credentialResponse);
        environmentResponse.setCloudPlatform("AWS");
        environmentResponse.setTunnel(Tunnel.DIRECT);
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.empty());
        credential = Credential.builder()
                .cloudPlatform("AWS")
                .build();
        when(freeIPAAdapter.getDomain(anyString())).thenReturn("cloudera.site");
    }

    @Test
    public void testConvert() {
        initMocks();
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        StackV4Request request = getRequest("stack.json");

        given(defaultCostTaggingService.prepareDefaultTags(anyString(), anyMap(), anyString())).willReturn(new HashMap<>());
        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(conversionService.convert(any(ClusterV4Request.class), eq(Cluster.class))).willReturn(new Cluster());
        given(telemetryConverter.convert(null, StackType.WORKLOAD)).willReturn(new Telemetry());
        // WHEN
        Stack stack = underTest.convert(request);
        // THEN
        assertAllFieldsNotNull(
                stack,
                Arrays.asList("description", "cluster", "environmentCrn", "gatewayPort", "useCcm", "network", "securityConfig",
                        "version", "created", "platformVariant", "cloudPlatform",
                        "customHostname", "customDomain", "clusterNameAsSubdomain", "hostgroupNameAsHostname", "parameters", "creator",
                        "environmentCrn", "terminated", "datalakeResourceId", "type", "inputs", "failurePolicy", "resourceCrn", "minaSshdServiceId"));
        assertEquals("eu-west-1", stack.getRegion());
        assertEquals("AWS", stack.getCloudPlatform());
        assertEquals("mystack", stack.getName());
        verify(telemetryConverter, times(1)).convert(null, StackType.WORKLOAD);
        verify(environmentClientService, times(1)).getByCrn(anyString());
        verify(gatewaySecurityGroupDecorator, times(1))
                .extendGatewaySecurityGroupWithDefaultGatewayCidrs(any(Stack.class), any(Tunnel.class));
    }

    @Test
    public void testConvertShouldHaveDefaultTags() throws IOException {
        initMocks();
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        StackV4Request request = getRequest("stack-without-tags.json");

        Map<String, String> defaultTags = Map.of(CB_USER_NAME.key(), "test", CB_VERSION.key(), "test", OWNER.key(), "test", CB_CREATION_TIMESTAMP.key(), "test");
        given(defaultCostTaggingService.prepareDefaultTags(anyString(), anyMap(), anyString())).willReturn(defaultTags);
        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(conversionService.convert(any(ClusterV4Request.class), eq(Cluster.class))).willReturn(new Cluster());
        // WHEN
        Stack stack = underTest.convert(request);
        // THEN
        assertAllFieldsNotNull(
                stack,
                Arrays.asList("description", "cluster", "environmentCrn", "gatewayPort", "useCcm", "network", "securityConfig",
                        "version", "created", "platformVariant", "cloudPlatform", "resourceCrn",
                        "customHostname", "customDomain", "clusterNameAsSubdomain", "hostgroupNameAsHostname", "parameters", "creator",
                        "environmentCrn", "terminated", "datalakeResourceId", "type", "inputs", "failurePolicy",  "minaSshdServiceId"));
        assertEquals("eu-west-1", stack.getRegion());
        assertEquals("AWS", stack.getCloudPlatform());
        assertEquals("mystack", stack.getName());
        assertEquals(defaultTags, stack.getTags().get(StackTags.class).getDefaultTags());
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
        ReflectionTestUtils.setField(underTest, "defaultRegions", "AWS:eu-west-2");
        given(defaultCostTaggingService.prepareDefaultTags(anyString(), anyMap(), anyString())).willReturn(new HashMap<>());
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("You can not modify the default user!");
        // WHEN
        Stack stack = underTest.convert(getRequest("stack-with-loginusername.json"));
        // THEN
        assertAllFieldsNotNull(
                stack,
                Arrays.asList("description", "statusReason", "cluster", "credential", "environmentCrn", "gatewayPort", "template", "network", "securityConfig",
                        "securityGroup", "version", "created", "platformVariant", "cloudPlatform", "saltPassword", "stackTemplate", "datalakeId",
                        "customHostname", "customDomain", "clusterNameAsSubdomain", "hostgroupNameAsHostname", "loginUserName", "rootVolumeSize"));
        assertEquals("eu-west-1", stack.getRegion());
    }

    @Test
    public void testConvertSharedServicePreparateWhenSharedServiceIsNullThenDatalakeNameShouldNotBeSet() {
        StackV4Request request = getRequest("stack.json");
        request.setCloudPlatform(CloudPlatform.MOCK);
        InstanceGroup instanceGroup = mock(InstanceGroup.class);
        when(instanceGroup.getInstanceGroupType()).thenReturn(InstanceGroupType.GATEWAY);

        //GIVEN
        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(conversionService.convert(any(InstanceGroupV4Request.class), eq(InstanceGroup.class))).willReturn(instanceGroup);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(conversionService.convert(any(ClusterV4Request.class), eq(Cluster.class))).willReturn(new Cluster());

        //WHEN
        Stack result = underTest.convert(request);

        //THEN
        Assert.assertNull(result.getDatalakeResourceId());
    }

    @Test
    public void testConvertSharedServicePreparateWhenThereIsNoDatalakeNameButSharedServiceIsNotNullThenThisDataShoudlBeTheDatalakeId() {
        Long expectedDataLakeId = 1L;
        StackV4Request request = getRequest("stack-with-shared-service.json");

        //GIVEN
        given(credentialClientService.getByName(anyString())).willReturn(credential);
        given(credentialClientService.getByCrn(anyString())).willReturn(credential);
        given(providerParameterCalculator.get(request)).willReturn(getMappable());
        given(conversionService.convert(any(ClusterV4Request.class), eq(Cluster.class))).willReturn(new Cluster());
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setId(expectedDataLakeId);
        given(datalakeResourcesService.getByNameForWorkspace(anyString(), any(Workspace.class))).willReturn(datalakeResources);

        //WHEN
        Stack result = underTest.convert(request);

        //THEN
        assertEquals(expectedDataLakeId, result.getDatalakeResourceId());
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
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<>(Collections.singletonList(instanceGroup)));

        given(conversionService.convert(any(StackAuthenticationV4Request.class), eq(StackAuthentication.class))).willReturn(new StackAuthentication());
        given(conversionService.convert(any(InstanceGroupV4Request.class), eq(InstanceGroup.class))).willReturn(instanceGroup);
    }
}
