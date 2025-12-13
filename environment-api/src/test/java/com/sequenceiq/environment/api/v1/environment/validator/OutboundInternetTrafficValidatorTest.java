package com.sequenceiq.environment.api.v1.environment.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;

@ExtendWith(MockitoExtension.class)
public class OutboundInternetTrafficValidatorTest {
    @InjectMocks
    private OutboundInternetTrafficValidator underTest;

    @ParameterizedTest
    @MethodSource("validOutboundInternetTrafficRequests")
    public void testValidOutboundInternetTrafficRequest(OutboundInternetTraffic outboundInternetTraffic, ServiceEndpointCreation serviceEndpointCreation) {
        EnvironmentNetworkRequest environmentNetworkRequest = new EnvironmentNetworkRequest();
        environmentNetworkRequest.setServiceEndpointCreation(serviceEndpointCreation);
        environmentNetworkRequest.setOutboundInternetTraffic(outboundInternetTraffic);
        assertTrue(underTest.isValid(environmentNetworkRequest, null));
    }

    @ParameterizedTest
    @MethodSource("invalidOutboundInternetTrafficRequests")
    public void testInvalidOutboundInternetTrafficRequest(OutboundInternetTraffic outboundInternetTraffic, ServiceEndpointCreation serviceEndpointCreation) {
        EnvironmentNetworkRequest environmentNetworkRequest = new EnvironmentNetworkRequest();
        environmentNetworkRequest.setServiceEndpointCreation(serviceEndpointCreation);
        environmentNetworkRequest.setOutboundInternetTraffic(outboundInternetTraffic);
        assertFalse(underTest.isValid(environmentNetworkRequest, null));
    }

    private static Stream<Arguments> validOutboundInternetTrafficRequests() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(null, ServiceEndpointCreation.DISABLED),
                Arguments.of(null, ServiceEndpointCreation.ENABLED),
                Arguments.of(OutboundInternetTraffic.ENABLED, null),
                Arguments.of(OutboundInternetTraffic.ENABLED, ServiceEndpointCreation.ENABLED),
                Arguments.of(OutboundInternetTraffic.ENABLED, ServiceEndpointCreation.DISABLED),
                Arguments.of(OutboundInternetTraffic.DISABLED, ServiceEndpointCreation.ENABLED));
    }

    private static Stream<Arguments> invalidOutboundInternetTrafficRequests() {
        return Stream.of(
                Arguments.of(OutboundInternetTraffic.DISABLED, null),
                Arguments.of(OutboundInternetTraffic.DISABLED, ServiceEndpointCreation.DISABLED));
    }
}
