package com.sequenceiq.cloudbreak.orchestrator.model;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;

import com.sequenceiq.cloudbreak.ccm.endpoint.HostEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointLookupException;

/**
 * Tests {@link GatewayConfig}.
 */
public class GatewayConfigTest {

    @Test
    public void testGetGatewayConfig() throws ServiceEndpointLookupException, InterruptedException {
        GatewayConfig gatewayConfig = createGatewayConfig(false);
        verifyGetGatewayConfigWithPassthroughEndpointFinder(gatewayConfig);
        verifyGetGatewayConfigWithLookupEndpointFinder(gatewayConfig);

        gatewayConfig = createGatewayConfig(true);
        verifyGetGatewayConfigWithPassthroughEndpointFinder(gatewayConfig);
        verifyGetGatewayConfigWithLookupEndpointFinder(gatewayConfig);
    }

    private void verifyGetGatewayConfigWithPassthroughEndpointFinder(GatewayConfig gatewayConfig) throws ServiceEndpointLookupException, InterruptedException {
        // Set up mock passthrough service endpoint finder
        ServiceEndpointFinder serviceEndpointFinder = mockServiceEndpointFinder(gatewayConfig.getConnectionAddress(), gatewayConfig.getGatewayPort());

        // Verify that the endpoint gateway config respects the connection address and port of the original gateway config
        GatewayConfig endpointGatewayConfig = gatewayConfig.getGatewayConfig(serviceEndpointFinder);
        verifyMatch(gatewayConfig, endpointGatewayConfig, null, null);
    }

    private void verifyGetGatewayConfigWithLookupEndpointFinder(GatewayConfig gatewayConfig) throws ServiceEndpointLookupException, InterruptedException {
        // Set up mock service endpoint finder
        String connectionAddress = RandomStringUtils.random(10);
        Integer gatewayPort = RandomUtils.nextInt();
        ServiceEndpointFinder serviceEndpointFinder = mockServiceEndpointFinder(connectionAddress, gatewayPort);

        // Verify that the endpoint gateway config respects the address and port returned by the service endpoint finder
        GatewayConfig endpointGatewayConfig = gatewayConfig.getGatewayConfig(serviceEndpointFinder);
        verifyMatch(gatewayConfig, endpointGatewayConfig, connectionAddress, gatewayPort);
    }

    private GatewayConfig createGatewayConfig(boolean longConstructor) {
        String connectionAddress = RandomStringUtils.randomAscii(10);
        String publicAddress = RandomStringUtils.random(10);
        String privateAddress = RandomStringUtils.random(10);
        int randomPort = RandomUtils.nextInt(0, 65535);
        Integer gatewayPort = (randomPort == 0) ? null : randomPort;
        String instanceId = RandomStringUtils.random(10);
        int randomValue = RandomUtils.nextInt(0, 3);
        Boolean knoxGatewayEnabled = (randomValue == 0) ? null : (randomValue == 1) ? Boolean.TRUE : Boolean.FALSE;
        if (longConstructor) {
            String hostname = RandomStringUtils.random(10);
            String serverCert = RandomStringUtils.random(10);
            String clientCert = RandomStringUtils.random(10);
            String clientKey = RandomStringUtils.random(10);
            String saltPassword = RandomStringUtils.random(10);
            String saltBootPassword = RandomStringUtils.random(10);
            String signatureKey = RandomStringUtils.random(10);
            boolean primary = RandomUtils.nextBoolean();
            String saltSignPrivateKey = RandomStringUtils.random(10);
            String saltSignPublicKey = RandomStringUtils.random(10);
            return new GatewayConfig(connectionAddress, publicAddress, privateAddress, hostname, gatewayPort, instanceId,
                    serverCert, clientCert, clientKey, saltPassword, saltBootPassword, signatureKey, knoxGatewayEnabled,
                    primary, saltSignPrivateKey, saltSignPublicKey, null, null, null);
        } else {
            return new GatewayConfig(connectionAddress, publicAddress, privateAddress, gatewayPort, instanceId, knoxGatewayEnabled);
        }
    }

    private ServiceEndpointFinder mockServiceEndpointFinder(String connectionAddress, Integer gatewayPort)
            throws ServiceEndpointLookupException, InterruptedException {
        ServiceEndpointFinder serviceEndpointFinder = mock(ServiceEndpointFinder.class);
        ServiceEndpoint serviceEndpoint = mock(ServiceEndpoint.class);
        HostEndpoint hostEndpoint = mock(HostEndpoint.class);
        when(hostEndpoint.getHostAddressString()).thenReturn(connectionAddress);
        when(serviceEndpoint.getHostEndpoint()).thenReturn(hostEndpoint);
        when(serviceEndpoint.getPort()).thenReturn(Optional.ofNullable(gatewayPort));
        when(serviceEndpointFinder.getServiceEndpoint(any())).thenReturn(serviceEndpoint);
        return serviceEndpointFinder;
    }

    private void verifyMatch(GatewayConfig gatewayConfig, GatewayConfig endpointGatewayConfig, String endpointConnectionAddress, Integer endpointPort) {
        String expectedConnectionAddress = (endpointConnectionAddress == null) ? gatewayConfig.getConnectionAddress() : endpointConnectionAddress;
        Integer expectedGatewayPort = (endpointPort == null) ? gatewayConfig.getGatewayPort() : endpointPort;
        String expectedGatewayUrl = String.format("https://%s:%d", expectedConnectionAddress, expectedGatewayPort);

        assertEquals(expectedConnectionAddress, endpointGatewayConfig.getConnectionAddress());
        assertEquals(gatewayConfig.getPublicAddress(), endpointGatewayConfig.getPublicAddress());
        assertEquals(gatewayConfig.getPrivateAddress(), endpointGatewayConfig.getPrivateAddress());
        assertEquals(gatewayConfig.getHostname(), endpointGatewayConfig.getHostname());
        assertEquals(expectedGatewayPort, endpointGatewayConfig.getGatewayPort());
        assertEquals(expectedGatewayUrl, endpointGatewayConfig.getGatewayUrl());
        assertEquals(gatewayConfig.getInstanceId(), endpointGatewayConfig.getInstanceId());
        assertEquals(gatewayConfig.getServerCert(), endpointGatewayConfig.getServerCert());
        assertEquals(gatewayConfig.getClientCert(), endpointGatewayConfig.getClientCert());
        assertEquals(gatewayConfig.getClientKey(), endpointGatewayConfig.getClientKey());
        assertEquals(gatewayConfig.getSaltPassword(), endpointGatewayConfig.getSaltPassword());
        assertEquals(gatewayConfig.getSaltBootPassword(), endpointGatewayConfig.getSaltBootPassword());
        assertEquals(gatewayConfig.getSignatureKey(), endpointGatewayConfig.getSignatureKey());
        assertEquals(gatewayConfig.getKnoxGatewayEnabled(), endpointGatewayConfig.getKnoxGatewayEnabled());
        assertEquals(gatewayConfig.isPrimary(), endpointGatewayConfig.isPrimary());
        assertEquals(gatewayConfig.getSaltSignPrivateKey(), endpointGatewayConfig.getSaltSignPrivateKey());
        assertEquals(gatewayConfig.getSaltSignPublicKey(), endpointGatewayConfig.getSaltSignPublicKey());
    }
}
