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
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

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
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackToStackV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.TelemetryConverter;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;

public class StackToStackV4ResponseConverterTest extends AbstractEntityConverterTest<Stack> {

    @InjectMocks
    private StackToStackV4ResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private ImageService imageService;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private TelemetryConverter telemetryConverter;

    @Mock
    private ProviderParameterCalculator providerParameterCalculator;

    @Mock
    private ServiceEndpointCollector serviceEndpointCollector;

    @Mock
    private DatalakeService datalakeService;

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
    }

    @Test
    public void testConvert() throws CloudbreakImageNotFoundException {

        Stack source = getSource();
        // GIVEN
        given(imageService.getImage(source.getId())).willReturn(mock(Image.class));
        given(conversionService.convert(any(Image.class), eq(StackImageV4Response.class))).willReturn(new StackImageV4Response());
        given(conversionService.convert(any(), eq(StackAuthenticationV4Response.class))).willReturn(new StackAuthenticationV4Response());
        given(conversionService.convert(any(), eq(CustomDomainSettingsV4Response.class))).willReturn(new CustomDomainSettingsV4Response());
        given(conversionService.convert(any(), eq(ClusterV4Response.class))).willReturn(new ClusterV4Response());
        given(conversionService.convert(any(), eq(NetworkV4Response.class))).willReturn(new NetworkV4Response());
        given(conversionService.convert(any(), eq(WorkspaceResourceV4Response.class))).willReturn(new WorkspaceResourceV4Response());
        given(conversionService.convert(any(), eq(CloudbreakDetailsV4Response.class))).willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(), eq(PlacementSettingsV4Response.class))).willReturn(new PlacementSettingsV4Response());
        given(conversionService.convert(any(), eq(TelemetryResponse.class))).willReturn(new TelemetryResponse());
        given(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupV4Response.class)).willReturn(new ArrayList<>());
        given(conversionService.convert(any(), eq(DatabaseResponse.class))).willReturn(new DatabaseResponse());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("gcp", "mock", "openstack", "aws", "yarn", "azure",
                "environmentName", "credentialName", "credentialCrn", "telemetry", "flowIdentifier"));
    }

    @Test
    public void testConvertWithoutCluster() throws CloudbreakImageNotFoundException {
        Stack source = getSource();
        // GIVEN
        getSource().setCluster(null);
        given(imageService.getImage(source.getId())).willReturn(mock(Image.class));
        given(conversionService.convert(any(Image.class), eq(StackImageV4Response.class))).willReturn(new StackImageV4Response());
        given(conversionService.convert(any(), eq(CustomDomainSettingsV4Response.class))).willReturn(new CustomDomainSettingsV4Response());
        given(conversionService.convert(any(), eq(StackAuthenticationV4Response.class))).willReturn(new StackAuthenticationV4Response());
        given(conversionService.convert(any(), eq(NetworkV4Response.class))).willReturn(new NetworkV4Response());
        given(conversionService.convert(any(), eq(WorkspaceResourceV4Response.class))).willReturn(new WorkspaceResourceV4Response());
        given(conversionService.convert(any(), eq(CloudbreakDetailsV4Response.class))).willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(), eq(PlacementSettingsV4Response.class))).willReturn(new PlacementSettingsV4Response());
        given(conversionService.convert(any(), eq(TelemetryResponse.class))).willReturn(new TelemetryResponse());
        given(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupV4Response.class)).willReturn(new ArrayList<>());
        given(conversionService.convert(any(), eq(DatabaseResponse.class))).willReturn(new DatabaseResponse());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "gcp", "mock", "openstack", "aws", "yarn", "azure",
                "telemetry", "environmentName", "credentialName", "credentialCrn", "telemetry", "flowIdentifier"));

        assertNull(result.getCluster());
    }

    @Test
    public void testConvertWithoutNetwork() throws CloudbreakImageNotFoundException {
        Stack source = getSource();
        // GIVEN
        getSource().setNetwork(null);
        given(imageService.getImage(source.getId())).willReturn(mock(Image.class));
        given(conversionService.convert(any(Image.class), eq(StackImageV4Response.class))).willReturn(new StackImageV4Response());
        given(conversionService.convert(any(), eq(CustomDomainSettingsV4Response.class))).willReturn(new CustomDomainSettingsV4Response());
        given(conversionService.convert(any(), eq(StackAuthenticationV4Response.class))).willReturn(new StackAuthenticationV4Response());
        given(conversionService.convert(any(), eq(ClusterV4Response.class))).willReturn(new ClusterV4Response());
        given(conversionService.convert(any(), eq(WorkspaceResourceV4Response.class))).willReturn(new WorkspaceResourceV4Response());
        given(conversionService.convert(any(), eq(CloudbreakDetailsV4Response.class))).willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(), eq(PlacementSettingsV4Response.class))).willReturn(new PlacementSettingsV4Response());
        given(conversionService.convert(any(), eq(TelemetryResponse.class))).willReturn(new TelemetryResponse());
        given(converterUtil.convertAll(source.getInstanceGroups(), InstanceGroupV4Response.class)).willReturn(new ArrayList<>());
        given(conversionService.convert(any(), eq(DatabaseResponse.class))).willReturn(new DatabaseResponse());
        // WHEN
        StackV4Response result = underTest.convert(source);
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("network", "gcp", "mock", "openstack", "aws", "yarn", "azure",
                "telemetry", "environmentName", "credentialName", "credentialCrn", "telemetry", "flowIdentifier"));

        assertNull(result.getNetwork());
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
        stack.setCloudPlatform("OPENSTACK");
        stack.setPlatformVariant(CloudConstants.OPENSTACK);
        stack.setGatewayPort(9443);
        stack.setCustomDomain("custom.domain");
        stack.setCustomHostname("hostname");
        stack.setStackAuthentication(new StackAuthentication());
        stack.getStackAuthentication().setPublicKey("rsakey");
        stack.getStackAuthentication().setLoginUserName("cloudbreak");
        stack.setHostgroupNameAsHostname(false);
        stack.setClusterNameAsSubdomain(false);
        stack.setDatalakeResourceId(1L);
        stack.setType(StackType.WORKLOAD);
        stack.setParameters(Map.of(PlatformParametersConsts.TTL_MILLIS, String.valueOf(System.currentTimeMillis())));
        Resource s3ArnResource = new Resource(ResourceType.S3_ACCESS_ROLE_ARN, "s3Arn", stack);
        stack.setResources(Collections.singleton(s3ArnResource));
        stack.setEnvironmentCrn("");
        stack.setTerminated(100L);
        stack.setExternalDatabaseCreationType(DatabaseAvailabilityType.HA);
        return stack;
    }
}
