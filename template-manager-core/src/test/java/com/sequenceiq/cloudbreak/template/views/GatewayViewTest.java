package com.sequenceiq.cloudbreak.template.views;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.domain.json.Json;

public class GatewayViewTest {

    @Test
    public void testInitializeGatewayView() {
        GatewayView gatewayView = new GatewayView(TestUtil.gatewayEnabled(), "/cb/secret/signkey");

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertEquals("topology", gatewayView.getTopologyName());
        Assert.assertEquals(new Json("{}"), gatewayView.getExposedServices());
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

}