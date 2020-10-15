package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;

import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.EndpointType;

public class ServiceEndpointCreationToEndpointTypeConverterTest {

    private ServiceEndpointCreationToEndpointTypeConverter underTest = new ServiceEndpointCreationToEndpointTypeConverter();

    @Test
    public void testWhenServiceEndpointCreationNull() {
        assertEquals(EndpointType.NONE, underTest.convert(null));
    }

    @Test
    public void testWhenServiceEndpointIsEnabledPrivateEndpoint() {
        assertEquals(EndpointType.USE_PRIVATE_ENDPOINT, underTest.convert(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT));
    }

    @Test
    public void testWhenServiceEndpointIsEnabledThenEndpointTypeServiceEndpoint() {
        assertEquals(EndpointType.USE_SERVICE_ENDPOINT, underTest.convert(ServiceEndpointCreation.ENABLED));
    }

    @Test
    public void testWhenServiceEndpointIsDisabledThenEndpointTypeNone() {
        assertEquals(EndpointType.NONE, underTest.convert(ServiceEndpointCreation.DISABLED));
    }

}
