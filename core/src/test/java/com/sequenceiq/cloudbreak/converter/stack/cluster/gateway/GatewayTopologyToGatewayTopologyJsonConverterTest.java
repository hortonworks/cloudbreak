package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.GatewayTopologyToGatewayTopologyV4RequestConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

class GatewayTopologyToGatewayTopologyJsonConverterTest {

    private final GatewayTopologyToGatewayTopologyV4RequestConverter underTest = new GatewayTopologyToGatewayTopologyV4RequestConverter();

    @Test
    void testConvert() {
        String topologyName = "topology1";
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setTopologyName(topologyName);
        ExposedServices exposedServices = new ExposedServices();
        exposedServices.setServices(List.of("SERVICE1", "SERVICE2"));
        gatewayTopology.setExposedServices(new Json(exposedServices));

        GatewayTopologyV4Request result = underTest.convert(gatewayTopology);

        assertEquals(topologyName, result.getTopologyName());
        assertEquals(2, result.getExposedServices().size());
        assertTrue(result.getExposedServices().containsAll(List.of("SERVICE1", "SERVICE2")));
    }
}
