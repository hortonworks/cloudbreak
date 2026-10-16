package com.sequenceiq.cloudbreak.orchestrator.salt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnectorExecutor;
import com.sequenceiq.cloudbreak.service.sslcontext.SSLContextProvider;

@ExtendWith(MockitoExtension.class)
class SaltServiceTest {

    @Mock
    private SSLContextProvider sslContextProvider;

    @Mock
    private SaltErrorResolver saltErrorResolver;

    @Mock
    private SaltConnectorExecutor saltConnectorExecutor;

    @InjectMocks
    private SaltService underTest;

    @Test
    void testCreateSaltConnectorBuildsConnectorForTheGateway() throws NoSuchAlgorithmException {
        GatewayConfig gatewayConfig = gatewayConfig("gw1");
        when(sslContextProvider.getSSLContext("server", Optional.empty(), "clientCert", "clientKey")).thenReturn(SSLContext.getDefault());

        try (SaltConnector result = underTest.createSaltConnector(gatewayConfig)) {
            assertEquals("gw1", result.getHostname());
            assertSame(saltErrorResolver, result.getSaltErrorResolver());
        }
    }

    @Test
    void testCreateSaltConnectorWithConnectAndReadTimeout() throws NoSuchAlgorithmException {
        when(sslContextProvider.getSSLContext("server", Optional.empty(), "clientCert", "clientKey")).thenReturn(SSLContext.getDefault());

        try (SaltConnector result = underTest.createSaltConnector(gatewayConfig("gw1"), 1000, 2000)) {
            assertEquals("gw1", result.getHostname());
        }
    }

    @Test
    void testCreateSaltConnectorWithCustomTimeout() throws NoSuchAlgorithmException {
        when(sslContextProvider.getSSLContext("server", Optional.empty(), "clientCert", "clientKey")).thenReturn(SSLContext.getDefault());

        try (SaltConnector result = underTest.createSaltConnectorWithCustomTimeout(gatewayConfig("gw1"), 1000, 2000, 3000)) {
            assertEquals("gw1", result.getHostname());
        }
    }

    @Test
    void testCreateSaltConnectorForMultipleGateways() throws NoSuchAlgorithmException {
        when(sslContextProvider.getSSLContext("server", Optional.empty(), "clientCert", "clientKey")).thenReturn(SSLContext.getDefault());

        List<SaltConnector> result = underTest.createSaltConnector(List.of(gatewayConfig("gw1"), gatewayConfig("gw2")));

        try {
            assertEquals(List.of("gw1", "gw2"), result.stream().map(SaltConnector::getHostname).toList());
        } finally {
            result.forEach(SaltConnector::close);
        }
    }

    @Test
    void testCreatedConnectorDelegatesToTheInjectedExecutor() throws NoSuchAlgorithmException {
        when(sslContextProvider.getSSLContext("server", Optional.empty(), "clientCert", "clientKey")).thenReturn(SSLContext.getDefault());
        GenericResponse expected = new GenericResponse();
        when(saltConnectorExecutor.health(any(SaltConnector.class))).thenReturn(expected);

        try (SaltConnector connector = underTest.createSaltConnector(gatewayConfig("gw1"))) {
            assertSame(expected, connector.health());
            verify(saltConnectorExecutor).health(connector);
        }
    }

    private GatewayConfig gatewayConfig(String hostname) {
        return GatewayConfig.builder().withServerCert("server").withClientCert("clientCert").withClientKey("clientKey")
                .withConnectionAddress("localhost").withGatewayPort(9443).withProtocol("https").withHostname(hostname).build();
    }

    @Test
    void testWhenNonPrimaryHasNewerSaltVersion() throws CloudbreakOrchestratorFailedException {
        GatewayConfig gw1 = GatewayConfig.builder().withInstanceId("gw1").withPrimary(true).withSaltVersion("1.0").build();
        GatewayConfig gw2 = GatewayConfig.builder().withInstanceId("gw2").withPrimary(true).withSaltVersion("2.0").build();
        GatewayConfig gw3 = GatewayConfig.builder().withInstanceId("gw3").withPrimary(false).withSaltVersion("3.0").build();

        GatewayConfig result = underTest.getPrimaryGatewayConfig(List.of(gw1, gw2, gw3));
        assertEquals("3.0", result.getSaltVersion().orElse(null));
        assertEquals("gw3", result.getInstanceId());
    }

    @Test
    void testLatestSaltVersionWhenNoPrimaryHasSaltVersion() throws CloudbreakOrchestratorFailedException {
        GatewayConfig gw1 = GatewayConfig.builder().withInstanceId("gw1").withPrimary(false).withSaltVersion("1.0").build();
        GatewayConfig gw2 = GatewayConfig.builder().withInstanceId("gw2").withPrimary(false).withSaltVersion("2.0").build();
        GatewayConfig gw3 = GatewayConfig.builder().withInstanceId("gw3").withPrimary(true).build();

        GatewayConfig result = underTest.getPrimaryGatewayConfig(List.of(gw1, gw2, gw3));
        assertEquals("2.0", result.getSaltVersion().orElse(null));
        assertEquals("gw2", result.getInstanceId());
    }

    @Test
    void testPrimaryWithoutSaltWhenNoSaltVersionSpecified() throws CloudbreakOrchestratorFailedException {
        GatewayConfig gw1 = GatewayConfig.builder().withInstanceId("gw1").withPrimary(true).build();
        GatewayConfig gw2 = GatewayConfig.builder().withInstanceId("gw2").withPrimary(false).build();
        GatewayConfig gw3 = GatewayConfig.builder().withInstanceId("gw3").withPrimary(false).build();

        GatewayConfig result = underTest.getPrimaryGatewayConfig(List.of(gw1, gw2, gw3));
        assertTrue(result.isPrimary());
        assertEquals("gw1", result.getInstanceId());
    }

    @Test
    void testNoPrimaryGatewayThrowsException() {
        GatewayConfig gw1 = GatewayConfig.builder().withInstanceId("gw1").withPrimary(false).build();
        GatewayConfig gw2 = GatewayConfig.builder().withInstanceId("gw2").withPrimary(false).build();
        GatewayConfig gw3 = GatewayConfig.builder().withInstanceId("gw3").withPrimary(false).build();

        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.getPrimaryGatewayConfig(List.of(gw1, gw2, gw3)));
    }

}