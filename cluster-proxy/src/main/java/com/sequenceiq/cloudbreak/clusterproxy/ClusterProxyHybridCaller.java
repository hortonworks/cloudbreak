package com.sequenceiq.cloudbreak.clusterproxy;

@FunctionalInterface
public interface ClusterProxyHybridCaller<T> {
    T call(String url, String userCrn, String environmentCrn);
}
