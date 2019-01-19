package com.sequenceiq.cloudbreak.converter.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@RunWith(MockitoJUnitRunner.class)
public class GatewayConvertUtilTest {

    private static final String DEPRECATED_TOPOLOGY_NAME = "deprecated";

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private final GatewayConvertUtil underTest = new GatewayConvertUtil();

    @Test
    public void testSsoTypeWhenNull() {
        GatewayJson source = new GatewayJson();
        Gateway result = new Gateway();

        underTest.setBasicProperties(source, result);

        assertEquals(SSOType.SSO_PROVIDER, result.getSsoType());
    }

    @Test
    public void testSsoTypeWhenNone() {
        GatewayJson source = new GatewayJson();
        source.setSsoType(SSOType.NONE);
        Gateway result = new Gateway();

        underTest.setBasicProperties(source, result);

        assertEquals(SSOType.NONE, result.getSsoType());
    }

    @Test
    public void testSsoTypeWhenSsoProvider() {
        GatewayJson source = new GatewayJson();
        source.setSsoType(SSOType.SSO_PROVIDER);
        Gateway result = new Gateway();

        underTest.setBasicProperties(source, result);

        assertEquals(SSOType.SSO_PROVIDER, result.getSsoType());
    }

    @Test
    public void testSetTopologiesWithLegacyRequest() {
        GatewayJson source = new GatewayJson();
        source.setTopologyName(DEPRECATED_TOPOLOGY_NAME);
        Gateway result = new Gateway();

        underTest.setTopologies(source, result);

        assertEquals(DEPRECATED_TOPOLOGY_NAME, result.getTopologies().iterator().next().getTopologyName());
    }

    @Test
    public void testSetMultipleTopologies() {
        GatewayJson source = new GatewayJson();

        GatewayTopologyJson topology1 = new GatewayTopologyJson();
        topology1.setTopologyName("topology1");
        topology1.setExposedServices(Collections.singletonList("AMBARI"));

        GatewayTopologyJson topology2 = new GatewayTopologyJson();
        topology2.setTopologyName("topology2");
        topology2.setExposedServices(Collections.singletonList("ALL"));

        source.setTopologies(Arrays.asList(topology1, topology2));
        Gateway result = new Gateway();

        when(conversionService.convert(any(), any())).thenReturn(new GatewayTopology());

        underTest.setTopologies(source, result);

        verify(conversionService, times(2)).convert(any(), any());
    }

    @Test
    public void testGatewayPathConversionWhenNoPathInGatewayJson() {
        Gateway gateway = new Gateway();
        GatewayJson gatewayJson = new GatewayJson();
        underTest.setGatewayPathAndSsoProvider("cluster-name", gatewayJson, gateway);

        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());
    }

    @Test
    public void testGatewayPathConversionWhenPathIsInGatewayJson() {
        Gateway gateway = new Gateway();
        String gatewayPath = "gatewayPath";
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setPath(gatewayPath);

        underTest.setGatewayPathAndSsoProvider("cluster-name", gatewayJson, gateway);

        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());

        assertEquals(gatewayPath, gateway.getPath());
        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());
    }

}