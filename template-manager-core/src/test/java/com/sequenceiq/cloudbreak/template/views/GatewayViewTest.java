package com.sequenceiq.cloudbreak.template.views;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class GatewayViewTest {

    @Test
    public void testInitializeGatewayView() throws JsonProcessingException {
        GatewayView gatewayView = new GatewayView(TestUtil.gatewayEnabled());

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertEquals("topology", gatewayView.getTopologyName());
        Assert.assertEquals(new Json("{}"), gatewayView.getExposedServices());
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

}