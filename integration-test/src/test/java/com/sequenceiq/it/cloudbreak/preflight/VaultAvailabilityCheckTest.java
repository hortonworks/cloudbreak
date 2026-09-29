package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.config.VaultProperties;
import com.sun.net.httpserver.HttpServer;

public class VaultAvailabilityCheckTest {

    private VaultProperties vaultProperties;

    private VaultAvailabilityCheck underTest;

    private HttpServer httpServer;

    @BeforeMethod
    public void setUp() {
        vaultProperties = mock(VaultProperties.class);

        underTest = new VaultAvailabilityCheck();
        ReflectionTestUtils.setField(underTest, "vaultProperties", vaultProperties);
        ReflectionTestUtils.setField(underTest, "reachabilityRestTemplateFactory", new ReachabilityRestTemplateFactory());
    }

    @AfterMethod
    public void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    public void testCheckPassesOnHealthyResponse() throws IOException {
        startServerReturning(200);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isTrue();
    }

    @Test(dataProvider = "unhealthyStatuses")
    public void testCheckFailsOnUnhealthyResponse(int status) throws IOException {
        startServerReturning(status);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }

    @DataProvider(name = "unhealthyStatuses")
    public Object[][] unhealthyStatuses() {
        return new Object[][]{{501}, {503}};
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
    public void testCheckFailsOnConnectionFailure() throws IOException {
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
