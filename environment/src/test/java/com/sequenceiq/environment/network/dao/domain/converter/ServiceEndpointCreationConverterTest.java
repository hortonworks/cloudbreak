package com.sequenceiq.environment.network.dao.domain.converter;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.environment.model.base.ServiceEndpointCreation;

public class ServiceEndpointCreationConverterTest {
    private final ServiceEndpointCreationConverter serviceEndpointCreationConverter = new ServiceEndpointCreationConverter();

    @Test
    public void testConvertToDatabaseColumnWhenEnabledIsReturnAsEnabled() {
        String defaultString = serviceEndpointCreationConverter.convertToDatabaseColumn(ServiceEndpointCreation.ENABLED);
        Assert.assertEquals("ENABLED", defaultString);
    }

    @Test
    public void testConvertToEntityAttributeWhenEnabledIsReturnAsEnabled() {
        ServiceEndpointCreation serviceEndpointCreation = serviceEndpointCreationConverter.convertToEntityAttribute("ENABLED");
        Assert.assertEquals(ServiceEndpointCreation.ENABLED, serviceEndpointCreation);
    }

    @Test
    public void testConvertToEntityAttributeWhenNotKnownIsReturnAsDisabled() {
        ServiceEndpointCreation serviceEndpointCreation = serviceEndpointCreationConverter.convertToEntityAttribute("not-known");
        Assert.assertEquals(ServiceEndpointCreation.DISABLED, serviceEndpointCreation);
    }
}
