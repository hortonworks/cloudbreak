package com.sequenceiq.cloudbreak.converter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToGatewayTopologyConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@ExtendWith(MockitoExtension.class)
class GatewayConvertUtilTest {

    @Mock
    private GatewayTopologyV4RequestToGatewayTopologyConverter gatewayTopologyV4RequestToGatewayTopologyConverter;

    @InjectMocks
    private GatewayConvertUtil underTest;

    @Test
    void testSetMultipleTopologies() {
        GatewayV4Request source = new GatewayV4Request();

        GatewayTopologyV4Request topology1 = new GatewayTopologyV4Request();
        topology1.setTopologyName("topology1");
        topology1.setExposedServices(Collections.singletonList("AMBARI"));

        GatewayTopologyV4Request topology2 = new GatewayTopologyV4Request();
        topology2.setTopologyName("topology2");
        topology2.setExposedServices(Collections.singletonList("ALL"));

        source.setTopologies(Arrays.asList(topology1, topology2));
        Gateway result = new Gateway();

        when(gatewayTopologyV4RequestToGatewayTopologyConverter.convert(any())).thenReturn(new GatewayTopology());

        underTest.setTopologies(source, result);

        verify(gatewayTopologyV4RequestToGatewayTopologyConverter, times(2)).convert(any());
    }

    @Test
    void testGatewayPathConversionWhenNoPathInGatewayJson() {
        Gateway gateway = new Gateway();
        GatewayV4Request gatewayJson = new GatewayV4Request();
        underTest.setGatewayPathAndSsoProvider(gatewayJson, gateway);

        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());
    }

    @Test
    void testGatewayPathConversionWhenPathIsInGatewayJson() {
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
