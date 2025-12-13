package com.sequenceiq.environment.network.dao.domain.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.type.ServiceEndpointCreation;

public class ServiceEndpointCreationConverterTest {
    private final ServiceEndpointCreationConverter serviceEndpointCreationConverter = new ServiceEndpointCreationConverter();

    @Test
    public void testConvertToDatabaseColumnWhenEnabledIsReturnAsEnabled() {
        String defaultString = serviceEndpointCreationConverter.convertToDatabaseColumn(ServiceEndpointCreation.ENABLED);
        assertEquals("ENABLED", defaultString);
    }

    @Test
    public void testConvertToEntityAttributeWhenEnabledIsReturnAsEnabled() {
        ServiceEndpointCreation serviceEndpointCreation = serviceEndpointCreationConverter.convertToEntityAttribute("ENABLED");
        assertEquals(ServiceEndpointCreation.ENABLED, serviceEndpointCreation);
    }

    @Test
    public void testConvertToEntityAttributeWhenNotKnownIsReturnAsDisabled() {
        ServiceEndpointCreation serviceEndpointCreation = serviceEndpointCreationConverter.convertToEntityAttribute("not-known");
        assertEquals(ServiceEndpointCreation.DISABLED, serviceEndpointCreation);
    }
}
