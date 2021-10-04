package com.sequenceiq.cloudbreak.converter.stack;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

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
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackTagsToTagsV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToPlacementSettingsV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.TelemetryConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.authentication.StackAuthenticationToStackAuthenticationV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterToClusterV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.customdomains.StackToCustomDomainsSettingsV4Response;
import com.sequenceiq.cloudbreak.converter.v4.stacks.database.DatabaseAvailabilityTypeToDatabaseResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.InstanceGroupToInstanceGroupV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.loadbalancer.LoadBalancerToLoadBalancerResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.network.NetworkToNetworkV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

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
    private DatabaseAvailabilityTypeToDatabaseResponseConverter databaseAvailabilityTypeToDatabaseResponseConverter;

    @Mock
    private RecipeToRecipeV4ResponseConverter recipeToRecipeV4ResponseConverter;

    @Mock
    private LoadBalancerToLoadBalancerResponseConverter loadBalancerToLoadBalancerResponseConverter;

    private CredentialResponse credentialResponse;

    @Before
    public void setUp() throws CloudbreakImageNotFoundException {
        underTest = new StackToStackV4ResponseConverter();
        MockitoAnnotations.initMocks(this);
        when(imageService.getImage(anyLong())).thenReturn(new Image("cb-centos66-amb200-2015-05-25", Collections.emptyMap(), "redhat6",
                "redhat6", "", "default", "default-id", new HashMap<>()));
        when(componentConfigProviderService.getCloudbreakDetails(anyLong())).thenReturn(new CloudbreakDetails("version"));
        when(componentConfigProviderService.getStackTemplate(anyLong())).thenReturn(new StackTemplate("{}", "version"));
        when(componentConfigProviderService.getTelemetry(anyLong())).thenReturn(new Telemetry());
        Mockito.doAnswer(answer  -> {
            StackV4Response result = answer.getArgument(1, StackV4Response.class);
            result.setSharedService(new SharedServiceV4Response());
            return null;
        }).when(datalakeService).addSharedServiceResponse(any(Stack.class), any(StackV4Response.class));
        when(serviceEndpointCollector.filterByStackType(any(StackType.class), any(List.class))).thenReturn(new ArrayList());
        credentialResponse = new CredentialResponse();
        credentialResponse.setName("cred-name");
        credentialResponse.setCrn("crn");
        when(loadBalancerService.findByStackId(any())).thenReturn(Set.of());
    }

    @Test
    public void testConvert() throws CloudbreakImageNotFoundException {

        Stack source = getSource();
        // GIVEN
        given(imageService.getImage(source.getId())).willReturn(mock(Image.class));
        given(imageToStackImageV4ResponseConverter.convert(any(Image.class))).willReturn(new StackImageV4Response());
        given(stackAuthenticationToStackAuthenticationV4ResponseConverter
                .convert(any(StackAuthentication.class))).willReturn(new StackAuthenticationV4Response());
        given(stackToCustomDomainsSettingsV4Response.convert(any())).willReturn(new CustomDomainSettingsV4Response());
        given(clusterToClusterV4ResponseConverter.convert(any())).willReturn(new ClusterV4Response());
        given(networkToNetworkV4ResponseConverter.convert(any())).willReturn(new NetworkV4Response());
        given(workspaceToWorkspaceResourceV4ResponseConverter.convert(any())).willReturn(new WorkspaceResourceV4Response());
        given(cloudbreakDetailsToCloudbreakDetailsV4ResponseConverter.convert(any())).willReturn(new CloudbreakDetailsV4Response());
        given(stackToPlacementSettingsV4ResponseConverter.convert(any())).willReturn(new PlacementSettingsV4Response());
        given(telemetryConverter.convert(any())).willReturn(new TelemetryResponse());
        given(instanceGroupToInstanceGroupV4ResponseConverter.convert(any())).willReturn(new InstanceGroupV4Response());
        given(databaseAvailabilityTypeToDatabaseResponseConverter.convert(any())).willReturn(new DatabaseResponse());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("gcp", "mock", "openstack", "aws", "yarn", "azure",
                "environmentName", "credentialName", "credentialCrn", "telemetry", "flowIdentifier", "loadBalancers"));

        verify(restRequestThreadLocalService).setWorkspace(source.getWorkspace());
    }

    @Test
    public void testConvertWithoutCluster() throws CloudbreakImageNotFoundException {
        Stack source = getSource();
        // GIVEN
        getSource().setCluster(null);
        given(imageService.getImage(source.getId())).willReturn(mock(Image.class));
        given(imageToStackImageV4ResponseConverter.convert(any())).willReturn(new StackImageV4Response());
        given(stackToCustomDomainsSettingsV4Response.convert(any())).willReturn(new CustomDomainSettingsV4Response());
        given(stackAuthenticationToStackAuthenticationV4ResponseConverter.convert(any())).willReturn(new StackAuthenticationV4Response());
        given(networkToNetworkV4ResponseConverter.convert(any())).willReturn(new NetworkV4Response());
        given(workspaceToWorkspaceResourceV4ResponseConverter.convert(any())).willReturn(new WorkspaceResourceV4Response());
        given(cloudbreakDetailsToCloudbreakDetailsV4ResponseConverter.convert(any())).willReturn(new CloudbreakDetailsV4Response());
        given(stackToPlacementSettingsV4ResponseConverter.convert(any())).willReturn(new PlacementSettingsV4Response());
        given(telemetryConverter.convert(any())).willReturn(new TelemetryResponse());
        given(instanceGroupToInstanceGroupV4ResponseConverter.convert(any())).willReturn(new InstanceGroupV4Response());
        given(databaseAvailabilityTypeToDatabaseResponseConverter.convert(any())).willReturn(new DatabaseResponse());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "gcp", "mock", "openstack", "aws", "yarn", "azure",
                "telemetry", "environmentName", "credentialName", "credentialCrn", "telemetry", "flowIdentifier", "loadBalancers"));

        assertNull(result.getCluster());
        verify(restRequestThreadLocalService).setWorkspace(source.getWorkspace());
    }

    @Test
    public void testConvertWithLoadBalancers() throws CloudbreakImageNotFoundException {
        Set<LoadBalancer> loadBalancers = Set.of(new LoadBalancer());

        Stack source = getSource();
        // GIVEN
        given(imageService.getImage(source.getId())).willReturn(mock(Image.class));
        given(imageToStackImageV4ResponseConverter.convert(any())).willReturn(new StackImageV4Response());
        given(stackToCustomDomainsSettingsV4Response.convert(any())).willReturn(new CustomDomainSettingsV4Response());
        given(stackAuthenticationToStackAuthenticationV4ResponseConverter.convert(any())).willReturn(new StackAuthenticationV4Response());
        given(networkToNetworkV4ResponseConverter.convert(any())).willReturn(new NetworkV4Response());
        given(workspaceToWorkspaceResourceV4ResponseConverter.convert(any())).willReturn(new WorkspaceResourceV4Response());
        given(cloudbreakDetailsToCloudbreakDetailsV4ResponseConverter.convert(any())).willReturn(new CloudbreakDetailsV4Response());
        given(stackToPlacementSettingsV4ResponseConverter.convert(any())).willReturn(new PlacementSettingsV4Response());
        given(telemetryConverter.convert(any())).willReturn(new TelemetryResponse());
        given(instanceGroupToInstanceGroupV4ResponseConverter.convert(any())).willReturn(new InstanceGroupV4Response());
        given(databaseAvailabilityTypeToDatabaseResponseConverter.convert(any())).willReturn(new DatabaseResponse());
        given(loadBalancerService.findByStackId(any())).willReturn(loadBalancers);
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertNotNull(result.getLoadBalancers());
        verify(restRequestThreadLocalService).setWorkspace(source.getWorkspace());
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
        stack.setEnvironmentCrn("");
        stack.setTerminated(100L);
        stack.setExternalDatabaseCreationType(DatabaseAvailabilityType.HA);
        return stack;
    }
}
