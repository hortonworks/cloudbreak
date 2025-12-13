package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyError;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;

class FreeIpaHealthCheckClusterProxyErrorRpcListenerTest {

    private final FreeIpaClusterProxyErrorRpcListener clusterProxyErrorRpcListener = new FreeIpaClusterProxyErrorRpcListener();

    @Test
    void testClusterProxyError() throws IOException {
        ClusterProxyError clusterProxyError = new ClusterProxyError("status", "cluster-proxy.proxy.timeout", "message", true);
        Response response = mock(Response.class);
        when(response.readEntity(any(Class.class))).thenReturn(clusterProxyError);
        assertThrows(ClusterProxyException.class, () -> {
            clusterProxyErrorRpcListener.onBeforeResponseProcessed(response);
        });
    }

    @Test
    void testClusterProxyNotAClusterProxyError() throws IOException {
        ClusterProxyError clusterProxyError = new ClusterProxyError("status", "error from something other than cluster proxy", "message", true);
        Response response = mock(Response.class);
        when(response.readEntity(any(Class.class))).thenReturn(clusterProxyError);
        assertDoesNotThrow(() -> {
            clusterProxyErrorRpcListener.onBeforeResponseProcessed(response);
        });
    }

    @Test
    void testClusterProxyNoError() throws IOException {
        Response response = mock(Response.class);
        when(response.readEntity(any(Class.class))).thenThrow(new RuntimeException("no cluster proxy error"));
        assertDoesNotThrow(() -> {
            clusterProxyErrorRpcListener.onBeforeResponseProcessed(response);
        });
    }
}
