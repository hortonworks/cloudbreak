package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.ClusterExposedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigResponse;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidator;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.mapper.ProxyConfigMapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariViewProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.util.StackUtil;

public class ClusterToClusterResponseConverterTest extends AbstractEntityConverterTest<Cluster> {

    @InjectMocks
    private ClusterToClusterResponseConverter underTest;

    @Mock
    private BlueprintValidator blueprintValidator;

    @Mock
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    @Mock
    private ConversionService conversionService;

    @Mock
    private JsonNode jsonNode;

    @Mock
    private JsonNode nameJsonNode;

    @Mock
    private Iterator<JsonNode> mockIterator;

    @Mock
    private Map<String, HostGroup> hostGroupMap;

    @Mock
    private HostGroup hostGroup;

    @Mock
    private InstanceGroup instanceGroup;

    @Mock
    private InstanceMetaData instanceMetaData;

    @Mock
    private Iterator<JsonNode> mockComponentIterator;

    @Mock
    private AmbariViewProvider ambariViewProvider;

    @Mock
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterComponentConfigProvider componentConfigProvider;

    @Mock
    private ProxyConfigMapper proxyConfigMapper;

    @Mock
    private ServiceEndpointCollector serviceEndpointCollector;

    @Mock
    private VaultService vaultService;

    private StackServiceComponentDescriptor stackServiceComponentDescriptor;

    @Before
    public void setUp() throws CloudbreakException {
        underTest = new ClusterToClusterResponseConverter();
        MockitoAnnotations.initMocks(this);
        given(orchestratorTypeResolver.resolveType(any(Orchestrator.class))).willReturn(OrchestratorType.HOST);
        given(rdsConfigService.findByClusterId(anyLong())).willReturn(new HashSet<>());
        given(stackService.findClustersConnectedToDatalake(anyLong())).willReturn(new HashSet<>());
        given(conversionService.convert(any(Workspace.class), eq(WorkspaceResourceResponse.class)))
                .willReturn(new WorkspaceResourceResponse());
        when(vaultService.resolveSingleValue(anyString())).then(returnsFirstArg());
        stackServiceComponentDescriptor = createStackServiceComponentDescriptor();
    }

    @Test
    public void testConvert() throws IOException {
        // GIVEN
        mockAll();
        getSource().setConfigStrategy(ConfigStrategy.NEVER_APPLY);
        given(stackUtil.extractAmbariIp(any(Stack.class))).willReturn("10.0.0.1");
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
        assertAllFieldsNotNull(result, Lists.newArrayList("cluster", "ambariStackDetails", "rdsConfigId", "blueprintCustomProperties",
                "blueprint", "rdsConfigs", "ldapConfig", "exposedKnoxServices", "customContainers", "extendedBlueprintText",
                "ambariRepoDetailsJson", "ambariDatabaseDetails", "creationFinished", "kerberosResponse", "fileSystemResponse"));
    }

    @Test
    public void testConvertWithoutUpSinceField() throws IOException {
        // GIVEN
        mockAll();
        getSource().setUpSince(null);
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(0L, result.getMinutesUp());
    }

    @Test
    public void testConvertWithoutMasterComponent() throws IOException {
        // GIVEN
        mockAll();
        given(stackServiceComponentDescs.get(anyString())).willReturn(new StackServiceComponentDescriptor("dummy", "dummy", 1, 1));
        // WHEN
        ClusterResponse result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
    }

    @Test
    public void testConvertWhenValidatorThrowException() throws IOException {
        // GIVEN
        given(blueprintValidator.getHostGroupNode(any(Blueprint.class))).willThrow(new IOException("error"));
        // WHEN
        underTest.convert(getSource());
        // THEN
        verify(blueprintValidator, times(0)).createHostGroupMap(anySet());

    }

    @Test
    public void testConvertWhenExtendedBlueprintTextIsNull() throws IOException {
        // GIVEN
        mockAll();
        // WHEN
        ClusterResponse clusterResponse = underTest.convert(getSource());
        // THEN
        assertNull(clusterResponse.getExtendedBlueprintText());

    }

    @Test
    public void testConvertWhenExtendedBlueprintTextIsNotNull() throws IOException {
        // GIVEN
        mockAll();
        getSource().setExtendedBlueprintText("extendedBlueprintText");
        // WHEN
        ClusterResponse clusterResponse = underTest.convert(getSource());
        // THEN
        assertEquals("extendedBlueprintText", clusterResponse.getExtendedBlueprintText());

    }

    @Test
    public void testExposedServices() throws IOException {
        mockAll();
        given(stackUtil.extractAmbariIp(any(Stack.class))).willReturn("10.0.0.1");
        ClusterResponse clusterResponse = underTest.convert(getSource());
        Map<String, Collection<ClusterExposedServiceResponse>> clusterExposedServicesForTopologies = clusterResponse.getClusterExposedServicesForTopologies();
        assertEquals(1L, clusterExposedServicesForTopologies.keySet().size());
        Collection<ClusterExposedServiceResponse> topology1ServiceList = clusterExposedServicesForTopologies.get("topology1");
        assertEquals(2L, topology1ServiceList.size());
    }

    @Override
    public Cluster createSource() {
        Stack stack = TestUtil.stack();
        Blueprint blueprint = TestUtil.blueprint();
        Cluster cluster = TestUtil.cluster(blueprint, stack, 1L);
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName("test");
        cluster.setProxyConfig(proxyConfig);
        stack.setCluster(cluster);
        Gateway gateway = new Gateway();
        cluster.setGateway(gateway);
        return cluster;
    }

    private void mockAll() throws IOException {
        when(ambariViewProvider.provideViewInformation(any(AmbariClient.class), any(Cluster.class))).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            return args[1];
        });
        given(blueprintValidator.getHostGroupNode(any(Blueprint.class))).willReturn(jsonNode);
        given(jsonNode.iterator()).willReturn(mockIterator);
        given(mockIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockIterator.next()).willReturn(jsonNode);
        given(conversionService.convert(any(RDSConfig.class), eq(RDSConfigJson.class))).willReturn(new RDSConfigRequest());
        given(conversionService.convert(any(Gateway.class), eq(GatewayJson.class))).willReturn(new GatewayJson());
        given(blueprintValidator.getHostGroupName(jsonNode)).willReturn("slave_1");
        given(blueprintValidator.createHostGroupMap(any(Set.class))).willReturn(hostGroupMap);
        given(hostGroupMap.get("slave_1")).willReturn(hostGroup);
        given(instanceGroup.getNotDeletedInstanceMetaDataSet()).willReturn(Sets.newHashSet(instanceMetaData));
        given(blueprintValidator.getComponentsNode(jsonNode)).willReturn(nameJsonNode);
        given(nameJsonNode.iterator()).willReturn(mockComponentIterator);
        given(mockComponentIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockComponentIterator.next()).willReturn(nameJsonNode);
        given(nameJsonNode.get(anyString())).willReturn(nameJsonNode);
        given(nameJsonNode.asText()).willReturn("dummyName");
        given(componentConfigProvider.getAmbariRepo(any(Set.class))).willReturn(null);
        given(stackServiceComponentDescs.get(anyString())).willReturn(stackServiceComponentDescriptor);
        ProxyConfigResponse proxyConfigResponse = new ProxyConfigResponse();
        proxyConfigResponse.setId(1L);
        given(proxyConfigMapper.mapEntityToResponse(any(ProxyConfig.class))).willReturn(proxyConfigResponse);
        given(serviceEndpointCollector.getAmbariServerUrl(any(), anyString())).willReturn("http://ambari.com");
        Map<String, Collection<ClusterExposedServiceResponse>> exposedServiceResponseMap = new HashMap<>();
        List<ClusterExposedServiceResponse> clusterExposedServiceResponseList = new ArrayList<>();
        ClusterExposedServiceResponse firstClusterExposedServiceResponse = new ClusterExposedServiceResponse();
        firstClusterExposedServiceResponse.setOpen(true);
        firstClusterExposedServiceResponse.setServiceUrl("http://service1");
        firstClusterExposedServiceResponse.setServiceName("serviceName1");
        firstClusterExposedServiceResponse.setKnoxService("knoxService1");
        firstClusterExposedServiceResponse.setDisplayName("displayName1");
        ClusterExposedServiceResponse secondClusterExposedServiceResponse = new ClusterExposedServiceResponse();
        clusterExposedServiceResponseList.add(firstClusterExposedServiceResponse);
        secondClusterExposedServiceResponse.setOpen(false);
        secondClusterExposedServiceResponse.setServiceUrl("http://service2");
        secondClusterExposedServiceResponse.setServiceName("serviceName2");
        secondClusterExposedServiceResponse.setKnoxService("knoxService2");
        secondClusterExposedServiceResponse.setDisplayName("displayName2");
        clusterExposedServiceResponseList.add(secondClusterExposedServiceResponse);
        exposedServiceResponseMap.put("topology1", clusterExposedServiceResponseList);
        given(serviceEndpointCollector.prepareClusterExposedServices(any(), anyString())).willReturn(exposedServiceResponseMap);
    }

    private StackServiceComponentDescriptor createStackServiceComponentDescriptor() {
        return new StackServiceComponentDescriptor("ELASTIC_SEARCH", "MASTER", 1, 1);
    }
}
