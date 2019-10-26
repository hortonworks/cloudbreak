package com.sequenceiq.cloudbreak.ccm.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

/**
 * Tests {@link DirectServiceEndpointFinder}
 */
public class DirectServiceEndpointFinderTest {

    /**
     * Verifies that the direct service endpoint finder acts as a passthrough,
     * whether port is specified or not.
     */
    @Test
    public void testGetServiceEndpoint() {
        String hostAddressString = randomHostname();

        verifyGetServiceEndpoint(hostAddressString, RandomUtils.nextInt(1, 65535));
        verifyGetServiceEndpoint(hostAddressString, null);
    }

    private void verifyGetServiceEndpoint(String hostAddressString, Integer port) {
        DirectServiceEndpointFinder endpointFinder = new DirectServiceEndpointFinder();

        ServiceEndpointRequest<HttpsServiceEndpoint> serviceEndpointRequest = createServiceEndpointRequest(hostAddressString, port);

        verifyGetServiceEndpoint(endpointFinder, serviceEndpointRequest, hostAddressString, port);
    }

    private ServiceEndpointRequest<HttpsServiceEndpoint> createServiceEndpointRequest(String hostAddressString, Integer port) {
        String targetInstanceId = RandomStringUtils.random(10);
        HostEndpoint hostEndpoint = mock(HostEndpoint.class);
        when(hostEndpoint.getHostAddressString()).thenReturn(hostAddressString);
        int randomValue = RandomUtils.nextInt(0, 2);
        ServiceFamily<HttpsServiceEndpoint> serviceFamily = (randomValue == 0) ? ServiceFamilies.GATEWAY : ServiceFamilies.KNOX;
        return ServiceEndpointRequest.createDefaultServiceEndpointRequest(targetInstanceId, hostEndpoint, port, serviceFamily, false);
    }

    private void verifyGetServiceEndpoint(DirectServiceEndpointFinder endpointFinder,
            ServiceEndpointRequest<HttpsServiceEndpoint> serviceEndpointRequest, String hostAddressString, Integer port) {
        ServiceEndpoint serviceEndpoint =
                endpointFinder.getServiceEndpoint(serviceEndpointRequest);
        assertEquals(hostAddressString, serviceEndpoint.getHostEndpoint().getHostAddressString());
        Optional<Integer> expectedPort =
                Optional.ofNullable(port).or(() -> Optional.of(serviceEndpointRequest.getServiceFamily().getDefaultPort()));
        assertEquals(expectedPort, serviceEndpoint.getPort());
        assertFalse(serviceEndpointRequest.isDirectAccessRequired());
    }

    private String randomHostname() {
        StringBuilder buf = new StringBuilder(randomHostnameComponent());
        for (int i = 0; i < RandomUtils.nextInt(0, 3); i++) {
            buf.append('.');
            buf.append(randomHostnameComponent());
        }
        return buf.toString();
    }

    private String randomHostnameComponent() {
        return RandomStringUtils.random(1, 32, 127, true, false)
                + RandomStringUtils.random(9, 32, 127, true, true);
    }
}
