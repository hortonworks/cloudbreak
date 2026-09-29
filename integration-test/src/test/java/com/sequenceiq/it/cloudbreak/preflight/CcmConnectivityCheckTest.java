package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.config.CcmProperties;

public class CcmConnectivityCheckTest {

    private CcmProperties ccmProperties;

    private CloudProviderEndpointResolver endpointResolver;

    private CcmConnectivityCheck underTest;

    @BeforeMethod
    public void setUp() {
        ccmProperties = mock(CcmProperties.class);
        endpointResolver = mock(CloudProviderEndpointResolver.class);

        underTest = new CcmConnectivityCheck();
        ReflectionTestUtils.setField(underTest, "ccmProperties", ccmProperties);
        ReflectionTestUtils.setField(underTest, "endpointResolver", endpointResolver);
    }

    @Test
    public void testCheckSkippedWhenMockProvider() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.MOCK);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    public void testCheckSkippedWhenHostBlank() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(ccmProperties.getHost()).thenReturn("");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    public void testCheckPassesWhenPortReachable() throws IOException {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(ccmProperties.getHost()).thenReturn("localhost");
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress("localhost", 0));
            when(ccmProperties.getPort()).thenReturn(serverSocket.getLocalPort());

            PreflightCheckResult result = underTest.check();

            assertThat(result.isPassed()).isTrue();
        }
    }

    @Test
    public void testCheckFailsWhenPortUnreachable() throws IOException {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(ccmProperties.getHost()).thenReturn("localhost");
        int closedPort;
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.bind(new InetSocketAddress("localhost", 0));
            closedPort = serverSocket.getLocalPort();
        }
        when(ccmProperties.getPort()).thenReturn(closedPort);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isPassed()).isFalse();
        assertThat(result.isSkipped()).isFalse();
    }
}
