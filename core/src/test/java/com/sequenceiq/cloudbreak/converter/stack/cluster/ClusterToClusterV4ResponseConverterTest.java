package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.common.model.OrchestratorType;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterToClusterV4ResponseConverter;
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
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariViewProvider;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

public class ClusterToClusterV4ResponseConverterTest extends AbstractEntityConverterTest<Cluster> {

    @InjectMocks
    private ClusterToClusterV4ResponseConverter underTest;

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
    private ServiceEndpointCollector serviceEndpointCollector;

    @Mock
    private BlueprintService blueprintService;

    @Before
    public void setUp() throws CloudbreakException {
        underTest = new ClusterToClusterV4ResponseConverter();
        MockitoAnnotations.initMocks(this);
        given(orchestratorTypeResolver.resolveType(any(Orchestrator.class))).willReturn(OrchestratorType.HOST);
        given(rdsConfigService.findByClusterId(anyLong())).willReturn(new HashSet<>());
        given(stackService.findClustersConnectedToDatalake(anyLong())).willReturn(new HashSet<>());
        given(conversionService.convert(any(Workspace.class), eq(WorkspaceResourceV4Response.class)))
                .willReturn(new WorkspaceResourceV4Response());
        given(blueprintService.isAmbariBlueprint(any())).willReturn(true);
    }

    @Test
    public void testConvert() throws IOException {
        // GIVEN
        mockAll();
        getSource().setConfigStrategy(ConfigStrategy.NEVER_APPLY);
        given(stackUtil.extractAmbariIp(any(Stack.class))).willReturn("10.0.0.1");
        Cluster source = getSource();
        TestUtil.setSecretField(Cluster.class, "cloudbreakAmbariUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "cloudbreakAmbariPassword", source, "pass", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpAmbariUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpAmbariPassword", source, "pass", "secret/path");
        when(conversionService.convert(any(String.class), any())).thenAnswer(invocation -> new SecretV4Response(null, invocation.getArgument(0)));
        // WHEN
        ClusterV4Response result = underTest.convert(source);
        // THEN
        assertEquals(1L, (long) result.getId());
        assertAllFieldsNotNull(result, Lists.newArrayList("cluster", "userName", "ambariStackDetails", "rdsConfigId", "blueprintCustomProperties",
                "blueprint", "rdsConfigs", "ldapConfig", "exposedKnoxServices", "customContainers", "extendedBlueprintText",
                "ambariRepoDetailsJson", "ambariDatabaseDetails", "creationFinished", "kerberosV4Response", "fileSystemResponse"));
    }

    @Test
    public void testConvertWithoutUpSinceField() throws IOException {
        // GIVEN
        mockAll();
        getSource().setUpSince(null);
        // WHEN
        ClusterV4Response result = underTest.convert(getSource());
        // THEN
        assertEquals(0L, result.getMinutesUp());
    }

    @Test
    public void testConvertWithoutMasterComponent() throws IOException {
        // GIVEN
        mockAll();
        // WHEN
        ClusterV4Response result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
    }

    @Test
    public void testConvertWhenExtendedBlueprintTextIsNull() throws IOException {
        // GIVEN
        mockAll();
        // WHEN
        ClusterV4Response clusterResponse = underTest.convert(getSource());
        // THEN
        assertNull(clusterResponse.getAmbari().getExtendedBlueprintText());

    }

    @Test
    public void testConvertWhenExtendedBlueprintTextIsNotNull() throws IOException {
        // GIVEN
        mockAll();
        getSource().setExtendedBlueprintText("extendedBlueprintText");
        // WHEN
        ClusterV4Response clusterResponse = underTest.convert(getSource());
        // THEN
        assertEquals("extendedBlueprintText", clusterResponse.getAmbari().getExtendedBlueprintText());

    }

    @Test
    public void testExposedServices() throws IOException {
        mockAll();
        given(stackUtil.extractAmbariIp(any(Stack.class))).willReturn("10.0.0.1");
        ClusterV4Response clusterResponse = underTest.convert(getSource());
        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesForTopologies = clusterResponse.getExposedServices();
        assertEquals(1L, clusterExposedServicesForTopologies.keySet().size());
        Collection<ClusterExposedServiceV4Response> topology1ServiceList = clusterExposedServicesForTopologies.get("topology1");
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
        given(jsonNode.iterator()).willReturn(mockIterator);
        given(mockIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockIterator.next()).willReturn(jsonNode);
        given(conversionService.convert(any(RDSConfig.class), eq(DatabaseV4Base.class))).willReturn(new DatabaseV4Request());
        given(conversionService.convert(any(Gateway.class), eq(GatewayV4Request.class))).willReturn(new GatewayV4Request());
        given(hostGroupMap.get("slave_1")).willReturn(hostGroup);
        given(instanceGroup.getNotDeletedInstanceMetaDataSet()).willReturn(Sets.newHashSet(instanceMetaData));
        given(nameJsonNode.iterator()).willReturn(mockComponentIterator);
        given(mockComponentIterator.hasNext()).willReturn(true).willReturn(false);
        given(mockComponentIterator.next()).willReturn(nameJsonNode);
        given(nameJsonNode.get(anyString())).willReturn(nameJsonNode);
        given(nameJsonNode.asText()).willReturn("dummyName");
        given(componentConfigProvider.getAmbariRepo(any(Set.class))).willReturn(null);
        ProxyV4Response proxyV4Response = new ProxyV4Response();
        proxyV4Response.setId(1L);
        given(serviceEndpointCollector.getAmbariServerUrl(any(), anyString())).willReturn("http://ambari.com");
        Map<String, Collection<ClusterExposedServiceV4Response>> exposedServiceResponseMap = new HashMap<>();
        List<ClusterExposedServiceV4Response> clusterExposedServiceResponseList = new ArrayList<>();
        ClusterExposedServiceV4Response firstClusterExposedServiceV4Response = new ClusterExposedServiceV4Response();
        firstClusterExposedServiceV4Response.setOpen(true);
        firstClusterExposedServiceV4Response.setServiceUrl("http://service1");
        firstClusterExposedServiceV4Response.setServiceName("serviceName1");
        firstClusterExposedServiceV4Response.setKnoxService("knoxService1");
        firstClusterExposedServiceV4Response.setDisplayName("displayName1");
        ClusterExposedServiceV4Response secondClusterExposedServiceV4Response = new ClusterExposedServiceV4Response();
        clusterExposedServiceResponseList.add(firstClusterExposedServiceV4Response);
        secondClusterExposedServiceV4Response.setOpen(false);
        secondClusterExposedServiceV4Response.setServiceUrl("http://service2");
        secondClusterExposedServiceV4Response.setServiceName("serviceName2");
        secondClusterExposedServiceV4Response.setKnoxService("knoxService2");
        secondClusterExposedServiceV4Response.setDisplayName("displayName2");
        clusterExposedServiceResponseList.add(secondClusterExposedServiceV4Response);
        exposedServiceResponseMap.put("topology1", clusterExposedServiceResponseList);
        given(serviceEndpointCollector.prepareClusterExposedServices(any(), anyString())).willReturn(exposedServiceResponseMap);
    }

    private StackServiceComponentDescriptor createStackServiceComponentDescriptor() {
        return new StackServiceComponentDescriptor("ELASTIC_SEARCH", "MASTER", 1, 1);
    }
}
