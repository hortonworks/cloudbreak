package com.sequenceiq.cloudbreak.converter.stack;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.flexsubscription.responses.FlexSubscriptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.customdomain.CustomDomainSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.placement.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.image.ImageService;

public class StackToStackV4ResponseConverterTest extends AbstractEntityConverterTest<Stack> {

    @InjectMocks
    private StackToStackV4ResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private ImageService imageService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ComponentConfigProvider componentConfigProvider;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private DatalakeResourcesService datalakeResourcesService;

    @Before
    public void setUp() throws CloudbreakImageNotFoundException {
        underTest = new StackToStackV4ResponseConverter();
        MockitoAnnotations.initMocks(this);
        when(imageService.getImage(anyLong())).thenReturn(new Image("cb-centos66-amb200-2015-05-25", Collections.emptyMap(), "redhat6",
                "redhat6", "", "default", "default-id", new HashMap<>()));
        when(componentConfigProvider.getCloudbreakDetails(anyLong())).thenReturn(new CloudbreakDetails("version"));
        when(componentConfigProvider.getStackTemplate(anyLong())).thenReturn(new StackTemplate("{}", "version"));
        when(clusterComponentConfigProvider.getHDPRepo(anyLong())).thenReturn(new StackRepoDetails());
        when(clusterComponentConfigProvider.getAmbariRepo(anyLong())).thenReturn(new AmbariRepo());
        DatalakeResources datalakeResources = new DatalakeResources();
        datalakeResources.setName("name");
        when(datalakeResourcesService.getDatalakeResourcesById(anyLong())).thenReturn(Optional.of(datalakeResources));
    }

    @Test
    public void testConvert() throws CloudbreakImageNotFoundException {

        Stack source = getSource();
        // GIVEN
        given(imageService.getImage(source.getId())).willReturn(mock(Image.class));
        given(conversionService.convert(any(Image.class), eq(StackImageV4Response.class))).willReturn(new StackImageV4Response());
        given(conversionService.convert(any(), eq(StackAuthenticationV4Response.class))).willReturn(new StackAuthenticationV4Response());
        given(conversionService.convert(any(), eq(EnvironmentSettingsV4Response.class))).willReturn(new EnvironmentSettingsV4Response());
        given(conversionService.convert(any(), eq(CustomDomainSettingsV4Response.class))).willReturn(new CustomDomainSettingsV4Response());
        given(conversionService.convert(any(), eq(FlexSubscriptionV4Response.class))).willReturn(new FlexSubscriptionV4Response());
        given(conversionService.convert(any(), eq(ClusterV4Response.class))).willReturn(new ClusterV4Response());
        given(conversionService.convert(any(), eq(NetworkV4Response.class))).willReturn(new NetworkV4Response());
        given(conversionService.convert(any(), eq(WorkspaceResourceV4Response.class))).willReturn(new WorkspaceResourceV4Response());
        given(conversionService.convert(any(), eq(CloudbreakDetailsV4Response.class))).willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(), eq(PlacementSettingsV4Response.class))).willReturn(new PlacementSettingsV4Response());
        given(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupV4Response.class)).willReturn(new ArrayList<>());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("gcp", "mock", "openstack", "aws", "yarn", "azure"));
    }

    @Test
    public void testConvertWithoutCluster() throws CloudbreakImageNotFoundException {
        Stack source = getSource();
        // GIVEN
        getSource().setCluster(null);
        given(imageService.getImage(source.getId())).willReturn(mock(Image.class));
        given(conversionService.convert(any(Image.class), eq(StackImageV4Response.class))).willReturn(new StackImageV4Response());
        given(conversionService.convert(any(), eq(EnvironmentSettingsV4Response.class))).willReturn(new EnvironmentSettingsV4Response());
        given(conversionService.convert(any(), eq(CustomDomainSettingsV4Response.class))).willReturn(new CustomDomainSettingsV4Response());
        given(conversionService.convert(any(), eq(FlexSubscriptionV4Response.class))).willReturn(new FlexSubscriptionV4Response());
        given(conversionService.convert(any(), eq(StackAuthenticationV4Response.class))).willReturn(new StackAuthenticationV4Response());
        given(conversionService.convert(any(), eq(CredentialV4Response.class))).willReturn(new CredentialV4Response());
        given(conversionService.convert(any(), eq(NetworkV4Response.class))).willReturn(new NetworkV4Response());
        given(conversionService.convert(any(), eq(WorkspaceResourceV4Response.class))).willReturn(new WorkspaceResourceV4Response());
        given(conversionService.convert(any(), eq(CloudbreakDetailsV4Response.class))).willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(), eq(PlacementSettingsV4Response.class))).willReturn(new PlacementSettingsV4Response());
        given(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupV4Response.class)).willReturn(new ArrayList<>());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "gcp", "mock", "openstack", "aws", "yarn", "azure"));

        assertNull(result.getCluster());
    }

    @Test
    public void testConvertWithoutNetwork() throws CloudbreakImageNotFoundException {
        Stack source = getSource();
        // GIVEN
        getSource().setNetwork(null);
        given(imageService.getImage(source.getId())).willReturn(mock(Image.class));
        given(conversionService.convert(any(Image.class), eq(StackImageV4Response.class))).willReturn(new StackImageV4Response());
        given(conversionService.convert(any(), eq(EnvironmentSettingsV4Response.class))).willReturn(new EnvironmentSettingsV4Response());
        given(conversionService.convert(any(), eq(CustomDomainSettingsV4Response.class))).willReturn(new CustomDomainSettingsV4Response());
        given(conversionService.convert(any(), eq(FlexSubscriptionV4Response.class))).willReturn(new FlexSubscriptionV4Response());
        given(conversionService.convert(any(), eq(StackAuthenticationV4Response.class))).willReturn(new StackAuthenticationV4Response());
        given(conversionService.convert(any(), eq(CredentialV4Response.class))).willReturn(new CredentialV4Response());
        given(conversionService.convert(any(), eq(ClusterV4Response.class))).willReturn(new ClusterV4Response());
        given(conversionService.convert(any(), eq(WorkspaceResourceV4Response.class))).willReturn(new WorkspaceResourceV4Response());
        given(conversionService.convert(any(), eq(CloudbreakDetailsV4Response.class))).willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(), eq(PlacementSettingsV4Response.class))).willReturn(new PlacementSettingsV4Response());
        given(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupV4Response.class)).willReturn(new ArrayList<>());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("network", "gcp", "mock", "openstack", "aws", "yarn", "azure"));

        assertNull(result.getNetwork());
    }

    @Override
    public Stack createSource() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.clusterDefinition(), stack, 1L);
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
        stack.setCloudPlatform("OPENSTACK");
        stack.setGatewayPort(9443);
        stack.setCustomDomain("custom.domain");
        stack.setCustomHostname("hostname");
        stack.setStackAuthentication(new StackAuthentication());
        stack.getStackAuthentication().setPublicKey("rsakey");
        stack.getStackAuthentication().setLoginUserName("cloudbreak");
        stack.setHostgroupNameAsHostname(false);
        stack.setClusterNameAsSubdomain(false);
        stack.setDatalakeResourceId(1L);
        stack.setParameters(Map.of(PlatformParametersConsts.TTL, String.valueOf(System.currentTimeMillis())));
        Resource s3ArnResource = new Resource(ResourceType.S3_ACCESS_ROLE_ARN, "s3Arn", stack);
        stack.setResources(Collections.singleton(s3ArnResource));
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setName("env");
        stack.setEnvironment(environmentView);
        stack.setTerminated(100L);
        stack.setFlexSubscription(new FlexSubscription());
        return stack;
    }
}
