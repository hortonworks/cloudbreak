package com.sequenceiq.freeipa.client.clusterproxy;

import java.util.Optional;

public abstract class ClusterProxyException extends RuntimeException {
    public abstract Optional<ClusterProxyError> getClusterProxyError();
}
