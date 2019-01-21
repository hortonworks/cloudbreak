package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.GatewayTopologyToGatewayTopologyV4RequestConverter;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

public class GatewayTopologyToGatewayTopologyJsonConverterTest {

    private final GatewayTopologyToGatewayTopologyV4RequestConverter underTest = new GatewayTopologyToGatewayTopologyV4RequestConverter();

    @Test
    public void testConvert() throws JsonProcessingException {
        String topologyName = "topology1";
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setTopologyName(topologyName);
        ExposedServices exposedServices = new ExposedServices();
        exposedServices.setServices(ExposedService.getAllKnoxExposed());
        gatewayTopology.setExposedServices(new Json(exposedServices));

        GatewayTopologyJson result = underTest.convert(gatewayTopology);

        assertEquals(topologyName, result.getTopologyName());
        assertEquals(ExposedService.getAllKnoxExposed().size(), result.getExposedServices().size());
        assertTrue(result.getExposedServices().containsAll(ExposedService.getAllKnoxExposed()));
    }
}