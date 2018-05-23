package com.sequenceiq.cloudbreak.converter.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.InjectMocks;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

public class GatewayConvertUtilTest {

    @InjectMocks
    private GatewayConvertUtil testedUtil = new GatewayConvertUtil();

    @Test
    public void testGatewayPathConversionWhenNoPathInGatewayJson() {
        Gateway gateway = new Gateway();
        GatewayJson gatewayJson = new GatewayJson();
        testedUtil.setGatewayPathAndSsoProvider("cluster-name", gatewayJson, gateway);

        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());
    }

    @Test
    public void testGatewayPathConversionWhenPathIsInGatewayJson() {
        Gateway gateway = new Gateway();
        String gatewayPath = "gatewayPath";
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setPath(gatewayPath);

        testedUtil.setGatewayPathAndSsoProvider("cluster-name", gatewayJson, gateway);

        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());

        assertEquals(gatewayPath, gateway.getPath());
        assertEquals('/' + gateway.getPath() + "/sso/api/v1/websso", gateway.getSsoProvider());
    }

}