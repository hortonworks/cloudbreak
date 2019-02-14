package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.responses.ProxyV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.clusterdefinition.validation.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterToClusterV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@RunWith(MockitoJUnitRunner.class)
public class ClusterToClusterV4ResponseConverterTest extends AbstractEntityConverterTest<Cluster> {

    @InjectMocks
    private ClusterToClusterV4ResponseConverter underTest;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ServiceEndpointCollector serviceEndpointCollector;

    @Mock
    private ClusterDefinitionService clusterDefinitionService;

    @Mock
    private ConverterUtil converterUtil;

    @Before
    public void setUp() {
        given(conversionService.convert(any(Workspace.class), eq(WorkspaceResourceV4Response.class)))
                .willReturn(new WorkspaceResourceV4Response());
        given(clusterDefinitionService.isAmbariBlueprint(any())).willReturn(true);
    }

    @Test
    public void testConvert() {
        // GIVEN
        getSource().setConfigStrategy(ConfigStrategy.NEVER_APPLY);
        given(stackUtil.extractAmbariIp(any(Stack.class))).willReturn("10.0.0.1");
        Cluster source = getSource();
        TestUtil.setSecretField(Cluster.class, "cloudbreakAmbariUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "cloudbreakAmbariPassword", source, "pass", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpAmbariUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpAmbariPassword", source, "pass", "secret/path");
        when(conversionService.convert(source.getProxyConfig(), ProxyV4Response.class)).thenReturn(new ProxyV4Response());
        // WHEN
        ClusterV4Response result = underTest.convert(source);
        // THEN
        assertEquals(1L, (long) result.getId());
        assertAllFieldsNotNull(result, Lists.newArrayList("ldap", "customContainers", "ambari", "creationFinished", "kerberos", "cloudStorage", "gateway"));
    }

    @Test
    public void testConvertWithoutUpSinceField() {
        // GIVEN
        getSource().setUpSince(null);
        // WHEN
        ClusterV4Response result = underTest.convert(getSource());
        // THEN
        assertEquals(0L, result.getMinutesUp());
    }

    @Test
    public void testConvertWithoutMasterComponent() {
        // GIVEN
        // WHEN
        ClusterV4Response result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
    }

    @Test
    public void testExposedServices() {
        Map<String, Collection<ClusterExposedServiceV4Response>> exposedServiceResponseMap = new HashMap<>();
        exposedServiceResponseMap.put("topology1", getExpiosedServices());
        given(serviceEndpointCollector.prepareClusterExposedServices(any(), anyString())).willReturn(exposedServiceResponseMap);

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
        ClusterDefinition clusterDefinition = TestUtil.clusterDefinition();
        Cluster cluster = TestUtil.cluster(clusterDefinition, stack, 1L);
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName("test");
        cluster.setProxyConfig(proxyConfig);
        stack.setCluster(cluster);
        Gateway gateway = new Gateway();
        cluster.setGateway(gateway);
        return cluster;
    }

    private List<ClusterExposedServiceV4Response> getExpiosedServices() {
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
        return clusterExposedServiceResponseList;
    }

    private StackServiceComponentDescriptor createStackServiceComponentDescriptor() {
        return new StackServiceComponentDescriptor("ELASTIC_SEARCH", "MASTER", 1, 1);
    }
}
