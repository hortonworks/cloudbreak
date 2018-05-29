package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;

@RunWith(MockitoJUnitRunner.class)
public class GatewayJsonValidatorTest {

    @Spy
    private GatewayConvertUtil gatewayConvertUtil;

    @InjectMocks
    private final GatewayJsonValidator underTest = new GatewayJsonValidator();

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
}