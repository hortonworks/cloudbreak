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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@RunWith(MockitoJUnitRunner.class)
public class GatewayConvertUtilTest {

    @Mock
    private ConverterUtil converterUtil;

    @InjectMocks
    private GatewayConvertUtil underTest;

    @Test
    public void testSetMultipleTopologies() {
        GatewayV4Request source = new GatewayV4Request();

        GatewayTopologyV4Request topology1 = new GatewayTopologyV4Request();
        topology1.setTopologyName("topology1");
        topology1.setExposedServices(Collections.singletonList("AMBARI"));

        GatewayTopologyV4Request topology2 = new GatewayTopologyV4Request();
        topology2.setTopologyName("topology2");
        topology2.setExposedServices(Collections.singletonList("ALL"));

        source.setTopologies(Arrays.asList(topology1, topology2));
        Gateway result = new Gateway();

        when(converterUtil.convert(any(), any())).thenReturn(new GatewayTopology());

        underTest.setTopologies(source, result);

        verify(converterUtil, times(2)).convert(any(), any());
    }

    @Test
    public void testGatewayPathConversionWhenNoPathInGatewayJson() {
        Gateway gateway = new Gateway();
        GatewayV4Request gatewayJson = new GatewayV4Request();
        underTest.setGatewayPathAndSsoProvider(gatewayJson, gateway);

        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());
    }

    @Test
    public void testGatewayPathConversionWhenPathIsInGatewayJson() {
        Gateway gateway = new Gateway();
        String gatewayPath = "gatewayPath";
        GatewayV4Request gatewayJson = new GatewayV4Request();
        gatewayJson.setPath(gatewayPath);

        underTest.setGatewayPathAndSsoProvider(gatewayJson, gateway);

        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());

        assertEquals(gatewayPath, gateway.getPath());
        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());
    }

}
