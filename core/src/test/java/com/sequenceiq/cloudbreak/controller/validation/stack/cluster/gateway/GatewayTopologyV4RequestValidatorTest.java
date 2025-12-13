package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import static com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceUtil.exposedService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@ExtendWith(MockitoExtension.class)
class GatewayTopologyV4RequestValidatorTest {

    @Mock
    private ExposedServiceListValidator exposedServiceListValidator;

    @InjectMocks
    private GatewayTopologyV4RequestValidator underTest;

    @Test
    void testWithNoTopologyName() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(1L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("topologyName must be set in gateway topology."));
    }

    @Test
    void testWithTopologyNameButNoServices() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName("topology");

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(State.VALID, result.getState());
    }

    @Test
    void testWithKnoxServiceButNoTopologyName() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setExposedServices(Collections.singletonList(exposedService("CLOUDERA_MANAGER_UI").getKnoxService()));

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(1L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains("topologyName must be set in gateway topology."));
    }

    @Test
    void testWithKnoxServiceAndTopologyName() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName("topology");
        gatewayTopologyJson.setExposedServices(Collections.singletonList(exposedService("CLOUDERA_MANAGER_UI").getKnoxService()));

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(State.VALID, result.getState());
    }

    @Test
    void testWithInvalidKnoxService() {
        String invalidService = "INVALID_SERVICE";
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName("topology");
        gatewayTopologyJson.setExposedServices(Collections.singletonList(invalidService));


        when(exposedServiceListValidator.validate(anyList())).thenReturn(new ValidationResultBuilder().error(invalidService).build());
        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(State.ERROR, result.getState());
        assertEquals(1L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains(invalidService));
    }

    @Test
    void testWithInvalidKnoxServiceAndNoTopologyName() {
        String invalidService = "INVALID_SERVICE";
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setExposedServices(Collections.singletonList(invalidService));

        when(exposedServiceListValidator.validate(anyList())).thenReturn(new ValidationResultBuilder().error(invalidService).build());

        ValidationResult result = underTest.validate(gatewayTopologyJson);

        assertEquals(State.ERROR, result.getState());
        assertEquals(2L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains(invalidService));
        assertTrue(result.getErrors().get(1).contains("topologyName must be set in gateway topology."));
    }
}
