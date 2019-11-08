package com.sequenceiq.freeipa.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;

import java.util.Optional;

public class ClusterProxyErrorRpcListener implements JsonRpcClient.RequestListener {

    private Optional<ClusterProxyError> deserializeAsClusteProxyError(ObjectNode objectNode) {
        ObjectMapper mapper = new ObjectMapper();
        ClusterProxyError clusterProxyError;
        try {
            clusterProxyError = mapper.treeToValue(objectNode, ClusterProxyError.class);
            if (!clusterProxyError.getCode().contains("cluster-proxy")) {
                return Optional.empty();
            }
        } catch (Exception ex) {
            return Optional.empty();
        }
        return Optional.of(clusterProxyError);
    }

    private void throwIfClusterProxyError(ObjectNode node) {
        Optional<ClusterProxyError> clusterProxyError = deserializeAsClusteProxyError(node);
        if (clusterProxyError.isPresent()) {
            throw new ClusterProxyException(String.format("Cluster proxy service returned error: %s", clusterProxyError.get()));
        }
    }

    @Override
    public void onBeforeRequestSent(JsonRpcClient client, ObjectNode request) {
        // no op
    }

    @Override
    public void onBeforeResponseProcessed(JsonRpcClient client, ObjectNode response) {
        throwIfClusterProxyError(response);
    }

}
