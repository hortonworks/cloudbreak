package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.config.ConversionConfig;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GatewayV4RequestValidator.class, ConversionConfig.class})
public class GatewayV4RequestValidatorTest {

    @Inject
    private GatewayV4RequestValidator underTest;

    @Test
    public void testValidationWithNameDuplicates() {
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
    public void testValidationWithoutNameDuplicates() {
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
    public void testValidationWithMissingTopology() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        ValidationResult result = underTest.validate(gatewayJson);

        assertEquals(State.ERROR, result.getState());
        assertTrue(result.getFormattedErrors().contains("No topology is defined in gateway request."));
    }
}