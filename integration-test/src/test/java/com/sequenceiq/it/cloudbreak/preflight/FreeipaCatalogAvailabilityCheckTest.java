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

import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sun.net.httpserver.HttpServer;

public class FreeipaCatalogAvailabilityCheckTest {

    private CommonCloudProperties commonCloudProperties;

    private FreeipaCatalogAvailabilityCheck underTest;

    private HttpServer httpServer;

    @BeforeMethod
    public void setUp() {
        commonCloudProperties = mock(CommonCloudProperties.class);

        underTest = new FreeipaCatalogAvailabilityCheck();
        ReflectionTestUtils.setField(underTest, "commonCloudProperties", commonCloudProperties);
        ReflectionTestUtils.setField(underTest, "reachabilityRestTemplateFactory", new ReachabilityRestTemplateFactory());
    }

    @AfterMethod
    public void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    public void testCheckSkippedWhenCatalogUrlBlank() {
        when(commonCloudProperties.getFreeipaImageCatalogUrl()).thenReturn(" ");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    public void testCheckPassesOnSuccessfulResponse() throws IOException {
        startServerReturning(200);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isTrue();
    }

    @Test(dataProvider = "unhealthyStatuses")
    public void testCheckFailsOnNon2xxHttpResponse(int status) throws IOException {
        startServerReturning(status);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }

    @DataProvider(name = "unhealthyStatuses")
    public Object[][] unhealthyStatuses() {
        return new Object[][]{{404}, {503}};
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
    public void testCheckFailsOnConnectionFailure() throws IOException {
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
