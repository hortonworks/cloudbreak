package com.sequenceiq.cloudbreak.converter.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.recipes.RecipeToRecipeV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.CloudbreakDetailsToCloudbreakDetailsV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.ImageToStackImageV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.ResourceToResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackTagsToTagsV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToPlacementSettingsV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.TelemetryConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.authentication.StackAuthenticationToStackAuthenticationV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterToClusterV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.customdomains.StackToCustomDomainsSettingsV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.database.ExternalDatabaseToDatabaseResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.loadbalancer.LoadBalancerToLoadBalancerResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.network.NetworkToNetworkV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

@ExtendWith(MockitoExtension.class)
public class StackToStackV4ResponseConverterTest extends AbstractEntityConverterTest<Stack> {

    @InjectMocks
    private StackToStackV4ResponseConverter underTest;

    @Mock
    private ImageService imageService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private TelemetryConverter telemetryConverter;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private ServiceEndpointCollector serviceEndpointCollector;

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private LoadBalancerPersistenceService loadBalancerService;

    @Mock
    private StackToPlacementSettingsV4ResponseConverter stackToPlacementSettingsV4ResponseConverter;

    @Mock
    private ImageToStackImageV4ResponseConverter imageToStackImageV4ResponseConverter;

    @Mock
    private StackTagsToTagsV4ResponseConverter stackTagsToTagsV4ResponseConverter;

    @Mock
    private ClusterToClusterV4ResponseConverter clusterToClusterV4ResponseConverter;

    @Mock
    private NetworkToNetworkV4ResponseConverter networkToNetworkV4ResponseConverter;

    @Mock
    private WorkspaceToWorkspaceResourceV4ResponseConverter workspaceToWorkspaceResourceV4ResponseConverter;

    @Mock
    private StackToCustomDomainsSettingsV4Response stackToCustomDomainsSettingsV4Response;

    @Mock
    private InstanceGroupToInstanceGroupV4ResponseConverter instanceGroupToInstanceGroupV4ResponseConverter;

    @Mock
    private CloudbreakDetailsToCloudbreakDetailsV4ResponseConverter cloudbreakDetailsToCloudbreakDetailsV4ResponseConverter;

    @Mock
    private StackAuthenticationToStackAuthenticationV4ResponseConverter stackAuthenticationToStackAuthenticationV4ResponseConverter;

    @Mock
    private ExternalDatabaseToDatabaseResponseConverter databaseAvailabilityTypeToDatabaseResponseConverter;

    @Mock
    private RecipeToRecipeV4ResponseConverter recipeToRecipeV4ResponseConverter;

    @Mock
    private LoadBalancerToLoadBalancerResponseConverter loadBalancerToLoadBalancerResponseConverter;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private ResourceToResourceV4ResponseConverter resourceToResourceV4ResponseConverter;

    private CredentialResponse credentialResponse;

    @BeforeEach
    public void setUp() throws CloudbreakImageNotFoundException {
        lenient().when(imageService.getImage(anyLong())).thenReturn(new Image("cb-centos66-amb200-2015-05-25", Collections.emptyMap(), "redhat6",
                "redhat6", "arch", "", "default", "default-id", new HashMap<>(), null, null, null));
        lenient().when(componentConfigProviderService.getCloudbreakDetails(anyLong())).thenReturn(new CloudbreakDetails("version"));
        lenient().when(componentConfigProviderService.getStackTemplate(anyLong())).thenReturn(new StackTemplate("{}", "version"));
        lenient().when(componentConfigProviderService.getTelemetry(anyLong())).thenReturn(new Telemetry());
        lenient().doAnswer(answer  -> {
            StackV4Response result = answer.getArgument(0, StackV4Response.class);
            result.setSharedService(new SharedServiceV4Response());
            return null;
        }).when(datalakeService).addSharedServiceResponse(any(StackV4Response.class));
        lenient().when(serviceEndpointCollector.filterByStackType(any(StackType.class), any(List.class))).thenReturn(Collections.emptyList());
        lenient().when(loadBalancerService.findByStackId(any())).thenReturn(Set.of());
    }

    @Test
    public void testConvert() throws CloudbreakImageNotFoundException {

        Stack source = getSource();
        // GIVEN
        when(imageService.getImage(source.getId())).thenReturn(mock(Image.class));
        when(imageToStackImageV4ResponseConverter.convert(any(Image.class))).thenReturn(new StackImageV4Response());
        when(stackAuthenticationToStackAuthenticationV4ResponseConverter
                .convert(any(StackAuthentication.class))).thenReturn(new StackAuthenticationV4Response());
        when(stackToCustomDomainsSettingsV4Response.convert(any(StackView.class))).thenReturn(new CustomDomainSettingsV4Response());
        when(clusterToClusterV4ResponseConverter.convert(any())).thenReturn(new ClusterV4Response());
        when(networkToNetworkV4ResponseConverter.convert(any())).thenReturn(new NetworkV4Response());
        when(workspaceToWorkspaceResourceV4ResponseConverter.convert(any())).thenReturn(new WorkspaceResourceV4Response());
        when(cloudbreakDetailsToCloudbreakDetailsV4ResponseConverter.convert(any())).thenReturn(new CloudbreakDetailsV4Response());
        when(stackToPlacementSettingsV4ResponseConverter.convert(any())).thenReturn(new PlacementSettingsV4Response());
        when(telemetryConverter.convert(any())).thenReturn(new TelemetryResponse());
        when(instanceGroupToInstanceGroupV4ResponseConverter.convert(any(), any(), any())).thenReturn(new InstanceGroupV4Response());
        when(databaseAvailabilityTypeToDatabaseResponseConverter.convert(any(), any())).thenReturn(new DatabaseResponse());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("gcp", "mock", "openstack", "aws", "yarn", "azure",
                "environmentName", "environmentType", "dataLakeV4Response", "credentialName", "credentialCrn",
                "telemetry", "flowIdentifier", "loadBalancers"));
        assertEquals(SeLinux.PERMISSIVE.name(), result.getSecurity().getSeLinux());
        verify(restRequestThreadLocalService).setWorkspaceId(source.getWorkspaceId());
    }

    @Test
    public void testConvertWithoutCluster() throws CloudbreakImageNotFoundException {
        Stack source = getSource();
        // GIVEN
        getSource().setCluster(null);
        when(imageService.getImage(source.getId())).thenReturn(mock(Image.class));
        when(imageToStackImageV4ResponseConverter.convert(any())).thenReturn(new StackImageV4Response());
        when(stackToCustomDomainsSettingsV4Response.convert(any(StackView.class))).thenReturn(new CustomDomainSettingsV4Response());
        when(stackAuthenticationToStackAuthenticationV4ResponseConverter.convert(any())).thenReturn(new StackAuthenticationV4Response());
        when(networkToNetworkV4ResponseConverter.convert(any())).thenReturn(new NetworkV4Response());
        when(workspaceToWorkspaceResourceV4ResponseConverter.convert(any())).thenReturn(new WorkspaceResourceV4Response());
        when(cloudbreakDetailsToCloudbreakDetailsV4ResponseConverter.convert(any())).thenReturn(new CloudbreakDetailsV4Response());
        when(stackToPlacementSettingsV4ResponseConverter.convert(any())).thenReturn(new PlacementSettingsV4Response());
        when(telemetryConverter.convert(any())).thenReturn(new TelemetryResponse());
        when(instanceGroupToInstanceGroupV4ResponseConverter.convert(any(), any(), any())).thenReturn(new InstanceGroupV4Response());
        when(databaseAvailabilityTypeToDatabaseResponseConverter.convert(any(), any())).thenReturn(new DatabaseResponse());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "gcp", "mock", "openstack", "aws", "yarn", "azure", "telemetry", "environmentName",
                "environmentType", "dataLakeV4Response", "credentialName", "credentialCrn", "telemetry", "flowIdentifier", "loadBalancers"));

        assertNull(result.getCluster());
        assertEquals(SeLinux.PERMISSIVE.name(), result.getSecurity().getSeLinux());
        verify(restRequestThreadLocalService).setWorkspaceId(source.getWorkspaceId());
    }

    @Test
    public void testConvertWithLoadBalancers() throws CloudbreakImageNotFoundException {
        Set<LoadBalancer> loadBalancers = Set.of(new LoadBalancer());

        Stack source = getSource();
        // GIVEN
        when(imageService.getImage(source.getId())).thenReturn(mock(Image.class));
        when(imageToStackImageV4ResponseConverter.convert(any())).thenReturn(new StackImageV4Response());
        when(stackToCustomDomainsSettingsV4Response.convert(any(StackView.class))).thenReturn(new CustomDomainSettingsV4Response());
        when(stackAuthenticationToStackAuthenticationV4ResponseConverter.convert(any())).thenReturn(new StackAuthenticationV4Response());
        when(networkToNetworkV4ResponseConverter.convert(any())).thenReturn(new NetworkV4Response());
        when(workspaceToWorkspaceResourceV4ResponseConverter.convert(any())).thenReturn(new WorkspaceResourceV4Response());
        when(cloudbreakDetailsToCloudbreakDetailsV4ResponseConverter.convert(any())).thenReturn(new CloudbreakDetailsV4Response());
        when(stackToPlacementSettingsV4ResponseConverter.convert(any())).thenReturn(new PlacementSettingsV4Response());
        when(telemetryConverter.convert(any())).thenReturn(new TelemetryResponse());
        when(instanceGroupToInstanceGroupV4ResponseConverter.convert(any(), any(), any())).thenReturn(new InstanceGroupV4Response());
        when(databaseAvailabilityTypeToDatabaseResponseConverter.convert(any(), any())).thenReturn(new DatabaseResponse());
        when(loadBalancerService.findByStackId(any())).thenReturn(loadBalancers);
        // WHEN
        source.setLoadBalancers(loadBalancers);
        StackV4Response result = underTest.convert(source);
        // THEN
        assertNotNull(result.getLoadBalancers());
        assertTrue(result.isEnableLoadBalancer());
        assertEquals(SeLinux.PERMISSIVE.name(), result.getSecurity().getSeLinux());
        verify(restRequestThreadLocalService).setWorkspaceId(source.getWorkspaceId());
    }

    @Override
    public Stack createSource() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        stack.setAvailabilityZone("avZone");
        Network network = new Network();
        network.setId(1L);
        stack.setNetwork(network);
        stack.setFailurePolicy(new FailurePolicy());
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setId(1L);
        orchestrator.setApiEndpoint("endpoint");
        orchestrator.setType("type");
        stack.setOrchestrator(orchestrator);
        stack.setParameters(new HashMap<>());
        stack.setCloudPlatform("AWS");
        stack.setPlatformVariant(CloudConstants.AWS);
        stack.setGatewayPort(9443);
        stack.setCustomDomain("custom.domain");
        stack.setCustomHostname("hostname");
        stack.setStackAuthentication(new StackAuthentication());
        stack.getStackAuthentication().setPublicKey("rsakey");
        stack.getStackAuthentication().setLoginUserName("cloudbreak");
        stack.setHostgroupNameAsHostname(false);
        stack.setClusterNameAsSubdomain(false);
        stack.setType(StackType.WORKLOAD);
        stack.setParameters(Map.of(PlatformParametersConsts.TTL_MILLIS, String.valueOf(System.currentTimeMillis())));
        Resource s3ArnResource = new Resource(ResourceType.S3_ACCESS_ROLE_ARN, "s3Arn", stack, "az1");
        stack.setResources(Collections.singleton(s3ArnResource));
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSeLinux(SeLinux.PERMISSIVE);
        stack.setSecurityConfig(securityConfig);
        stack.setEnvironmentCrn("");
        stack.setTerminated(100L);
        stack.setDatalakeCrn("");
        stack.setJavaVersion(11);
        Database database = new Database();
        database.setExternalDatabaseAvailabilityType(DatabaseAvailabilityType.HA);
        stack.setDatabase(database);
        stack.setSupportedImdsVersion("v2");
        stack.setArchitecture(Architecture.X86_64);
        return stack;
    }
}
