package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.config.ConversionConfig;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;

@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = {GatewayV4RequestValidator.class, ConversionConfig.class})
class GatewayV4RequestValidatorTest {

    @InjectMocks
    private GatewayV4RequestValidator underTest;

    @Test
    void testValidationWithNameDuplicates() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        GatewayTopologyV4Request topology1 = new GatewayTopologyV4Request();
        topology1.setTopologyName("Apple");
        GatewayTopologyV4Request topology2 = new GatewayTopologyV4Request();
        topology2.setTopologyName("Apple");
        GatewayTopologyV4Request topology3 = new GatewayTopologyV4Request();
        topology3.setTopologyName("Banana");
        gatewayJson.setTopologies(Arrays.asList(topology1, topology2, topology3));
        ValidationResult result = underTest.validate(gatewayJson);

        assertEquals(State.ERROR, result.getState());
        assertTrue(result.getFormattedErrors().contains("Apple"));
    }

    @Test
    void testValidationWithoutNameDuplicates() {
        GatewayV4Request gatewayJsonV4Request = new GatewayV4Request();
        GatewayTopologyV4Request topology1 = new GatewayTopologyV4Request();
        topology1.setTopologyName("Apple");
        GatewayTopologyV4Request topology2 = new GatewayTopologyV4Request();
        topology2.setTopologyName("Banana");
        GatewayTopologyV4Request topology3 = new GatewayTopologyV4Request();
        topology3.setTopologyName("Citrone");
        gatewayJsonV4Request.setTopologies(Arrays.asList(topology1, topology2, topology3));
        ValidationResult result = underTest.validate(gatewayJsonV4Request);

        assertEquals(State.VALID, result.getState());
    }

    @Test
    void testValidationWithMissingTopology() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        ValidationResult result = underTest.validate(gatewayJson);

        assertEquals(State.ERROR, result.getState());
        assertTrue(result.getFormattedErrors().contains("No topology is defined in gateway request."));
    }
}
