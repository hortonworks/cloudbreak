package com.sequenceiq.cloudbreak.clusterproxy;

import com.fasterxml.jackson.core.type.TypeReference;

public class ClusterProxyErrorTypeReference extends TypeReference<ClusterProxyError> {

    public static ClusterProxyErrorTypeReference get() {
        return new ClusterProxyErrorTypeReference();
    }
}
