package com.sequenceiq.freeipa.client;

import java.util.Optional;

import javax.ws.rs.core.Response;

import com.sequenceiq.cloudbreak.client.RpcListener;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyError;
import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyException;

public class FreeIpaClusterProxyErrorRpcListener implements RpcListener {

    private Optional<ClusterProxyError> deserializeAsClusterProxyError(Response response) {
        try {
            ClusterProxyError clusterProxyError = response.readEntity(ClusterProxyError.class);
            if (clusterProxyError.getCode().contains("cluster-proxy")) {
                return Optional.of(clusterProxyError);
            } else {
                return Optional.empty();
            }
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private void throwIfClusterProxyError(Response response) {
        Optional<ClusterProxyError> clusterProxyError = deserializeAsClusterProxyError(response);
        if (clusterProxyError.isPresent()) {
            throw new ClusterProxyException(String.format("Cluster proxy service returned error: %s", clusterProxyError.get()), clusterProxyError);
        }
    }

    @Override
    public void onBeforeResponseProcessed(Response response) throws Exception {
        throwIfClusterProxyError(response);
    }
}
