package com.sequenceiq.cloudbreak.converter.stack;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CloudbreakDetailsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.authentication.StackAuthenticationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
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
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.StackAuthentication;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;

public class StackToStackResponseConverterTest extends AbstractEntityConverterTest<Stack> {

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

    @Before
    public void setUp() throws CloudbreakImageNotFoundException {
        underTest = new StackToStackV4ResponseConverter();
        MockitoAnnotations.initMocks(this);
        when(imageService.getImage(anyLong())).thenReturn(new Image("cb-centos66-amb200-2015-05-25", Collections.emptyMap(), "redhat6",
            "redhat6", "", "default", "default-id", new HashMap<>()));
        when(componentConfigProvider.getCloudbreakDetails(anyLong())).thenReturn(new CloudbreakDetails("version"));
        when(componentConfigProvider.getStackTemplate(anyLong())).thenReturn(new StackTemplate("{}", "version"));
        when(clusterComponentConfigProvider.getHDPRepo(anyLong())).thenReturn(new StackRepoDetails());
        when(clusterComponentConfigProvider.getAmbariDatabase(anyLong())).thenReturn(new AmbariDatabase());
        when(clusterComponentConfigProvider.getAmbariRepo(anyLong())).thenReturn(new AmbariRepo());
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(conversionService.convert(any(), any()))
                .willReturn(new ImageV4Response())
                .willReturn(new StackAuthenticationV4Response())
                .willReturn(new CredentialV4Response())
                .willReturn(new ClusterV4Response())
                .willReturn(new NetworkV4Response())
                .willReturn(new WorkspaceResourceV4Response())
                .willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupV4Request>());
        // WHEN
        StackV4Response result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("terminated", "platformVariant", "ambariVersion", "hdpVersion", "flexSubscription", "owner", "account"));
    }

    @Test
    public void testConvertWithoutCredential() {
        // GIVEN
        given(conversionService.convert(any(), any()))
                .willReturn(new ImageV4Response())
                .willReturn(new StackAuthenticationV4Response())
                .willReturn(new CredentialV4Response())
                .willReturn(new ClusterV4Response())
                .willReturn(new NetworkV4Response())
                .willReturn(new WorkspaceResourceV4Response())
                .willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupV4Request>());
        // WHEN
        StackV4Response result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("terminated", "credentialId", "cloudPlatform", "platformVariant", "ambariVersion", "hdpVersion",
                "stackTemplate", "cloudbreakDetails", "flexSubscription", "owner", "account"));
    }

    @Test
    public void testConvertWithoutCluster() {
        // GIVEN
        getSource().setCluster(null);
        given(conversionService.convert(any(), any()))
                .willReturn(new ImageV4Response())
                .willReturn(new StackAuthentication())
                .willReturn(new CredentialV4Response())
                .willReturn(new NetworkV4Response())
                .willReturn(new WorkspaceResourceV4Response())
                .willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupV4Request>());
        getSource().setCluster(null);
        // WHEN
        StackV4Response result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("terminated", "cluster", "platformVariant", "ambariVersion", "hdpVersion", "flexSubscription",
                "owner", "account"));
    }

    @Test
    public void testConvertWithoutFailurePolicy() {
        // GIVEN
        getSource().setFailurePolicy(null);
        given(conversionService.convert(any(), any()))
                .willReturn(new ImageV4Response())
                .willReturn(new StackAuthenticationV4Response())
                .willReturn(new CredentialV4Response())
                .willReturn(new ClusterV4Response())
                .willReturn(new NetworkV4Response())
                .willReturn(new WorkspaceResourceV4Response())
                .willReturn(new CloudbreakDetailsV4Response())
                .willReturn(new CredentialV4Response())
                .willReturn(new NetworkV4Response());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupV4Request>());
        // WHEN
        StackV4Response result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("terminated", "failurePolicy", "platformVariant", "ambariVersion", "hdpVersion", "stackTemplate",
                "cloudbreakDetails", "flexSubscription", "owner", "account"));
    }

    @Test
    public void testConvertWithoutNetwork() {
        // GIVEN
        getSource().setNetwork(null);
        given(conversionService.convert(any(), any()))
                .willReturn(new ImageV4Response())
                .willReturn(new StackAuthenticationV4Response())
                .willReturn(new CredentialV4Response())
                .willReturn(new ClusterV4Response())
                .willReturn(new WorkspaceResourceV4Response())
                .willReturn(new CloudbreakDetailsV4Response());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupV4Request>());
        // WHEN
        StackV4Response result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("terminated", "networkId", "platformVariant", "ambariVersion", "hdpVersion", "network",
                "flexSubscription", "owner", "account"));
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
        stack.setGatewayPort(9443);
        stack.setCustomDomain("custom.domain");
        stack.setCustomHostname("hostname");
        stack.setStackAuthentication(new StackAuthentication());
        stack.getStackAuthentication().setPublicKey("rsakey");
        stack.getStackAuthentication().setLoginUserName("cloudbreak");
        stack.setHostgroupNameAsHostname(false);
        stack.setClusterNameAsSubdomain(false);
        Resource s3ArnResource = new Resource(ResourceType.S3_ACCESS_ROLE_ARN, "s3Arn", stack);
        stack.setResources(Collections.singleton(s3ArnResource));
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setName("env");
        stack.setEnvironment(environmentView);
        return stack;
    }
}
