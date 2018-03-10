package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStatesTest;

public class DefaultRouteResponseTest {

    @Test
    public void testParseAndFindGatewayInterfaceName() throws IOException {
        InputStream responseStream = SaltStatesTest.class.getResourceAsStream("/default_route_response.json");
        String json = IOUtils.toString(responseStream);
        DefaultRouteResponse response = new ObjectMapper().readValue(json, DefaultRouteResponse.class);
        Assert.assertEquals("eth0", response.getGatewayInterfaceName());
    }
}
