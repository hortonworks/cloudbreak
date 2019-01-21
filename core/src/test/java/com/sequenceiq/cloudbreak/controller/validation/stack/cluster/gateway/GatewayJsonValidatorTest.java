package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.conf.ConversionConfig;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GatewayV4RequestValidator.class, ConversionConfig.class, GatewayConvertUtil.class})
public class GatewayJsonValidatorTest {

    @Inject
    private GatewayV4RequestValidator underTest;

    @Test
    public void testValidationWithNameDuplicates() {
        GatewayJson gatewayJson = new GatewayJson();
        GatewayTopologyJson topology1 = new GatewayTopologyJson();
        topology1.setTopologyName("Apple");
        GatewayTopologyJson topology2 = new GatewayTopologyJson();
        topology2.setTopologyName("Apple");
        GatewayTopologyJson topology3 = new GatewayTopologyJson();
        topology3.setTopologyName("Banana");
        gatewayJson.setTopologies(Arrays.asList(topology1, topology2, topology3));
        ValidationResult result = underTest.validate(gatewayJson);

        assertEquals(State.ERROR, result.getState());
        assertTrue(result.getFormattedErrors().contains("Apple"));
    }

    @Test
    public void testValidationWithoutNameDuplicates() {
        GatewayJson gatewayJson = new GatewayJson();
        GatewayTopologyJson topology1 = new GatewayTopologyJson();
        topology1.setTopologyName("Apple");
        GatewayTopologyJson topology2 = new GatewayTopologyJson();
        topology2.setTopologyName("Banana");
        GatewayTopologyJson topology3 = new GatewayTopologyJson();
        topology3.setTopologyName("Citrone");
        gatewayJson.setTopologies(Arrays.asList(topology1, topology2, topology3));
        ValidationResult result = underTest.validate(gatewayJson);

        assertEquals(State.VALID, result.getState());
    }

    @Test
    public void testValidationWithMissingTopology() {
        GatewayJson gatewayJson = new GatewayJson();
        ValidationResult result = underTest.validate(gatewayJson);

        assertEquals(State.ERROR, result.getState());
        assertTrue(result.getFormattedErrors().contains("No topology is defined in gateway request."));
    }

    @Test
    public void testValidationWithDeprecatedExposedServices() {
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setExposedServices(Collections.singletonList("ALL"));
        ValidationResult result = underTest.validate(gatewayJson);

        assertEquals(State.VALID, result.getState());
    }

    @Test
    public void testValidationWithFalseGatewayEnabled() {
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setEnableGateway(false);
        ValidationResult result = underTest.validate(gatewayJson);

        assertEquals(State.VALID, result.getState());
    }

    @Test
    public void testValidationWithNullGatewayEnabled() {
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setEnableGateway(null);
        gatewayJson.setExposedServices(Collections.singletonList("ALL"));
        ValidationResult result = underTest.validate(gatewayJson);

        assertEquals(State.VALID, result.getState());
    }
}