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

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sun.net.httpserver.HttpServer;

@ExtendWith(MockitoExtension.class)
class FreeipaCatalogAvailabilityCheckTest {

    @Mock
    private CommonCloudProperties commonCloudProperties;

    @Spy
    private ReachabilityRestTemplateFactory reachabilityRestTemplateFactory = new ReachabilityRestTemplateFactory();

    @InjectMocks
    private FreeipaCatalogAvailabilityCheck underTest;

    private HttpServer httpServer;

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void testCheckSkippedWhenCatalogUrlBlank() {
        when(commonCloudProperties.getFreeipaImageCatalogUrl()).thenReturn(" ");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    void testCheckPassesOnSuccessfulResponse() throws IOException {
        startServerReturning(200);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 503})
    void testCheckFailsOnNon2xxHttpResponse(int status) throws IOException {
        startServerReturning(status);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }

    private void startServerReturning(int status) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/v3-test-freeipa-image-catalog.json", exchange -> {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        httpServer.start();
        when(commonCloudProperties.getFreeipaImageCatalogUrl())
                .thenReturn("http://localhost:" + httpServer.getAddress().getPort() + "/v3-test-freeipa-image-catalog.json");
    }

    @Test
    void testCheckFailsOnConnectionFailure() throws IOException {
        int closedPort;
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress("localhost", 0));
            closedPort = serverSocket.getLocalPort();
        }
        when(commonCloudProperties.getFreeipaImageCatalogUrl())
                .thenReturn("http://localhost:" + closedPort + "/v3-test-freeipa-image-catalog.json");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }
}
