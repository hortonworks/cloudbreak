package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;

@RunWith(MockitoJUnitRunner.class)
public class GatewayTopologyJsonValidatorTest {

    private final GatewayTopologyV4RequestValidator underTest = new GatewayTopologyV4RequestValidator(new ExposedServiceListValidator());

    @Test
    public void testWithNoTopologyName() {
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(1L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("topologyName must be set in gateway topology."));
    }

    @Test
    public void testWithTopologyNameButNoServices() {
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
        gatewayTopologyJson.setTopologyName("topology");

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(State.VALID, result.getState());
    }

    @Test
    public void testWithKnoxServiceButNoTopologyName() {
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
        gatewayTopologyJson.setExposedServices(Collections.singletonList(ExposedService.AMBARI.getKnoxService()));

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(1L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("topologyName must be set in gateway topology."));
    }

    @Test
    public void testWithKnoxServiceAndTopologyName() {
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
        gatewayTopologyJson.setTopologyName("topology");
        gatewayTopologyJson.setExposedServices(Collections.singletonList(ExposedService.AMBARI.getKnoxService()));

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(State.VALID, result.getState());
    }

    @Test
    public void testWithInvalidKnoxService() {
        String invalidService = "INVALID_SERVICE";
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
        gatewayTopologyJson.setTopologyName("topology");
        gatewayTopologyJson.setExposedServices(Collections.singletonList(invalidService));

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(State.ERROR, result.getState());
        assertEquals(1L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains(invalidService));
    }

    @Test
    public void testWithInvalidKnoxServiceAndNoTopologyName() {
        String invalidService = "INVALID_SERVICE";
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
        gatewayTopologyJson.setExposedServices(Collections.singletonList(invalidService));

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(State.ERROR, result.getState());
        assertEquals(2L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains(invalidService));
        assertTrue(result.getErrors().get(1).contains("topologyName must be set in gateway topology."));
    }
}