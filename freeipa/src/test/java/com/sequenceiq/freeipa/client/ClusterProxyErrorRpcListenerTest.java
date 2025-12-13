package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;

class ClusterProxyErrorRpcListenerTest {

    private static final String JSON
            = "{\"status\":504,\"code\":\"cluster-proxy.proxy.timeout\",\"message\":\"Error message\"}";

    private static final String JSON_MISSING_STATUS_FIELD
            = "{\"code\":\"cluster-proxy.proxy.timeout\",\"message\":\"Error message\"}";

    private static final String JSON_MISSING_CODE_FIELD
            = "{\"status\":504,\"message\":\"Error message\"}";

    private static final String JSON_CODE_WITHOUT_CLUSTER_PROXY_PREFIX
            = "{\"status\":504,\"code\":\"asdf\",\"message\":\"Error message\"}";

    private final ClusterProxyErrorRpcListener clusterProxyErrorRpcListener = new ClusterProxyErrorRpcListener();

    private ObjectNode toObjectNode(String jsonString) throws IOException {
        return (ObjectNode) new ObjectMapper().readTree(jsonString);
    }

    @Test
    void testClusterProxyError() throws IOException {
        ObjectNode jsonNode = toObjectNode(JSON);
        assertThrows(ClusterProxyException.class, () -> {
            clusterProxyErrorRpcListener.onBeforeResponseProcessed(null, jsonNode);
        });
    }

    @Test
    void testClusterProxyErrorMissingField() throws IOException {
        ObjectNode jsonNode = toObjectNode(JSON_MISSING_STATUS_FIELD);
        assertThrows(ClusterProxyException.class, () -> {
            clusterProxyErrorRpcListener.onBeforeResponseProcessed(null, jsonNode);
        });
    }

    @Test
    void testClusterProxyErrorNoStatusCode() throws IOException {
        ObjectNode jsonNode = toObjectNode(JSON_MISSING_CODE_FIELD);
        clusterProxyErrorRpcListener.onBeforeResponseProcessed(null, jsonNode);
    }

    @Test
    void testClusterProxyErroCodeWithoutClusterProxyPrefix() throws IOException {
        ObjectNode jsonNode = toObjectNode(JSON_CODE_WITHOUT_CLUSTER_PROXY_PREFIX);
        clusterProxyErrorRpcListener.onBeforeResponseProcessed(null, jsonNode);
    }
}
