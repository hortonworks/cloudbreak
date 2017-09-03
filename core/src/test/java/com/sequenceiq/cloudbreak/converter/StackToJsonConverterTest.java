package com.sequenceiq.cloudbreak.converter;

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
import com.sequenceiq.cloudbreak.api.model.CloudbreakDetailsJson;
import com.sequenceiq.cloudbreak.api.model.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.CredentialResponse;
import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.api.model.ImageJson;
import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.NetworkResponse;
import com.sequenceiq.cloudbreak.api.model.OrchestratorResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cloud.model.HDPRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.StackTemplate;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import com.sequenceiq.cloudbreak.domain.Network;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.image.ImageService;

public class StackToJsonConverterTest extends AbstractEntityConverterTest<Stack> {

    @InjectMocks
    private StackToJsonConverter underTest;

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
        underTest = new StackToJsonConverter();
        MockitoAnnotations.initMocks(this);
        when(imageService.getImage(anyLong())).thenReturn(new Image("testimage", new HashMap<>()));
        when(componentConfigProvider.getCloudbreakDetails(anyLong())).thenReturn(new CloudbreakDetails("version"));
        when(componentConfigProvider.getStackTemplate(anyLong())).thenReturn(new StackTemplate("{}", "version"));
        when(clusterComponentConfigProvider.getHDPRepo(anyLong())).thenReturn(new HDPRepo());
        when(clusterComponentConfigProvider.getAmbariDatabase(anyLong())).thenReturn(new AmbariDatabase());
        when(clusterComponentConfigProvider.getAmbariRepo(anyLong())).thenReturn(new AmbariRepo());
    }

    @Test
    public void testConvert() {
        // GIVEN
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new ImageJson())
                .willReturn(new CredentialResponse())
                .willReturn(new ClusterResponse())
                .willReturn(new FailurePolicyResponse())
                .willReturn(new NetworkResponse())
                .willReturn(new OrchestratorResponse())
                .willReturn(new CloudbreakDetailsJson());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupRequest>());
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("platformVariant", "ambariVersion", "hdpVersion", "flexSubscription"));
    }

    @Test
    public void testConvertWithoutCredential() {
        // GIVEN
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new ImageJson())
                .willReturn(new CredentialResponse())
                .willReturn(new ClusterResponse())
                .willReturn(new FailurePolicyResponse())
                .willReturn(new NetworkResponse())
                .willReturn(new OrchestratorResponse())
                .willReturn(new CloudbreakDetailsJson());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupRequest>());
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("credentialId", "cloudPlatform", "platformVariant", "ambariVersion", "hdpVersion",
                "stackTemplate", "cloudbreakDetails", "flexSubscription"));
    }

    @Test
    public void testConvertWithoutCluster() {
        // GIVEN
        getSource().setCluster(null);
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new ImageJson())
                .willReturn(new CredentialResponse())
                .willReturn(new FailurePolicyResponse())
                .willReturn(new NetworkResponse())
                .willReturn(new OrchestratorResponse())
                .willReturn(new CloudbreakDetailsJson());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupRequest>());
        getSource().setCluster(null);
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("cluster", "platformVariant", "ambariVersion", "hdpVersion", "flexSubscription"));
    }

    @Test
    public void testConvertWithoutFailurePolicy() {
        // GIVEN
        getSource().setFailurePolicy(null);
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new ImageJson())
                .willReturn(new CredentialResponse())
                .willReturn(new ClusterResponse())
                .willReturn(new NetworkResponse())
                .willReturn(new OrchestratorResponse())
                .willReturn(new CloudbreakDetailsJson())
                .willReturn(new CredentialResponse())
                .willReturn(new NetworkResponse());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupRequest>());
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("failurePolicy", "platformVariant", "ambariVersion", "hdpVersion", "stackTemplate",
                "cloudbreakDetails", "flexSubscription"));
    }

    @Test
    public void testConvertWithoutNetwork() {
        // GIVEN
        getSource().setNetwork(null);
        given(conversionService.convert(any(Object.class), any(Class.class)))
                .willReturn(new ImageJson())
                .willReturn(new CredentialResponse())
                .willReturn(new ClusterResponse())
                .willReturn(new FailurePolicyResponse())
                .willReturn(new OrchestratorResponse())
                .willReturn(new CloudbreakDetailsJson());
        given(conversionService.convert(any(Object.class), any(TypeDescriptor.class), any(TypeDescriptor.class)))
                .willReturn(new HashSet<InstanceGroupRequest>());
        // WHEN
        StackResponse result = underTest.convert(getSource());
        // THEN
        assertAllFieldsNotNull(result, Arrays.asList("networkId", "platformVariant", "ambariVersion", "hdpVersion", "network", "flexSubscription"));
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
        stack.setPublicKey("rsakey");
        stack.setLoginUserName("cloudbreak");
        stack.setHostgroupNameAsHostname(false);
        stack.setClusterNameAsSubdomain(false);
        Resource s3ArnResource = new Resource(ResourceType.S3_ACCESS_ROLE_ARN, "s3Arn", stack);
        stack.setResources(Collections.singleton(s3ArnResource));
        return stack;
    }
}
