package com.sequenceiq.it.cloudbreak.preflight;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.cloudbreak.config.CcmProperties;

@ExtendWith(MockitoExtension.class)
class CcmConnectivityCheckTest {

    @Mock
    private CcmProperties ccmProperties;

    @Mock
    private CloudProviderEndpointResolver endpointResolver;

    @InjectMocks
    private CcmConnectivityCheck underTest;

    @Test
    void testCheckSkippedWhenMockProvider() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.MOCK);

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    void testCheckSkippedWhenHostBlank() {
        when(endpointResolver.activeCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(ccmProperties.getHost()).thenReturn("");

        PreflightCheckResult result = underTest.check();

        assertThat(result.isSkipped()).isTrue();
    }

    @Test
    void testCheckPassesWhenPortReachable() throws IOException {
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
    void testCheckFailsWhenPortUnreachable() throws IOException {
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
