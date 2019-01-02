package com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;

public class ExposedServiceListValidatorTest {

    private final ExposedServiceListValidator underTest = new ExposedServiceListValidator();

    @Test
    public void testWithValidKnoxServices() {
        List<String> services = ExposedService.getAllKnoxExposed();
        ValidationResult result = underTest.validate(services);
        assertEquals(State.VALID, result.getState());
    }

    @Test
    public void testWithAnInvalidKnoxService() {
        List<String> services = new ArrayList<>(ExposedService.getAllKnoxExposed());
        String invalidService = "INVALID_SERVICE";
        services.add(invalidService);

        ValidationResult result = underTest.validate(services);

        assertEquals(State.ERROR, result.getState());
        assertEquals(1L, result.getErrors().size());
        assertTrue(result.getErrors().get(0).contains(invalidService));
    }
}