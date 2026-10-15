package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.it.cloudbreak.config.VaultProperties;
import com.sun.net.httpserver.HttpServer;

@ExtendWith(MockitoExtension.class)
class VaultAvailabilityCheckTest {

    @Mock
    private VaultProperties vaultProperties;

    @Spy
    private ReachabilityRestTemplateFactory reachabilityRestTemplateFactory = new ReachabilityRestTemplateFactory();

    @InjectMocks
    private VaultAvailabilityCheck underTest;

    private HttpServer httpServer;

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void testCheckPassesOnHealthyResponse() throws IOException {
        startServerReturning(200);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {501, 503})
    void testCheckFailsOnUnhealthyResponse(int status) throws IOException {
        startServerReturning(status);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }

    private void startServerReturning(int status) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/v1/sys/health", exchange -> {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        httpServer.start();
        when(vaultProperties.getHealthUrl()).thenReturn("http://localhost:" + httpServer.getAddress().getPort() + "/v1/sys/health");
    }

    @Test
    void testCheckFailsOnConnectionFailure() throws IOException {
        int closedPort;
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress("localhost", 0));
            closedPort = serverSocket.getLocalPort();
        }
        when(vaultProperties.getHealthUrl()).thenReturn("http://localhost:" + closedPort + "/v1/sys/health");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }
}
