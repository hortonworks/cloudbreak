package com.sequenceiq.cloudbreak.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.common.model.PrivateEndpointType;

public class ServiceEndpointCreationToEndpointTypeConverterTest {

    private final ServiceEndpointCreationToEndpointTypeConverter underTest = new ServiceEndpointCreationToEndpointTypeConverter();

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    public void testWhenServiceEndpointCreationNull(CloudPlatform platform) {
        assertEquals(PrivateEndpointType.NONE, underTest.convert(null, platform.name()));
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    public void testWhenServiceEndpointIsEnabledPrivateEndpoint(CloudPlatform platform) {
        assertEquals(PrivateEndpointType.USE_PRIVATE_ENDPOINT, underTest.convert(ServiceEndpointCreation.ENABLED_PRIVATE_ENDPOINT, platform.name()));
    }

    @Test
    public void testWhenServiceEndpointIsEnabledThenEndpointTypeVPCEndpointForAWS() {
        assertEquals(PrivateEndpointType.USE_VPC_ENDPOINT, underTest.convert(ServiceEndpointCreation.ENABLED, CloudPlatform.AWS.name()));
    }

    @Test
    public void testWhenServiceEndpointIsEnabledThenEndpointTypeNoneForAzure() {
        assertEquals(PrivateEndpointType.NONE, underTest.convert(ServiceEndpointCreation.ENABLED, CloudPlatform.AZURE.name()));
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    public void testWhenServiceEndpointIsDisabledThenEndpointTypeNone(CloudPlatform platform) {
        assertEquals(PrivateEndpointType.NONE, underTest.convert(ServiceEndpointCreation.DISABLED, platform.name()));
    }

}
