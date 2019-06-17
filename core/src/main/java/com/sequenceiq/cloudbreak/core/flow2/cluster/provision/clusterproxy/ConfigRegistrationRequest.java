package com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy;

import java.util.List;

class ConfigRegistrationRequest {
    private String clusterCrn;

    private List<ClusterServiceConfig> services;

    ConfigRegistrationRequest(String clusterCrn, List<ClusterServiceConfig> services) {
        this.clusterCrn = clusterCrn;
        this.services = services;
    }

    @Override
    public String toString() {
        return "ConfigRegistrationRequest{clusterCrn='" + clusterCrn + '\'' + ", services=" + services + '}';
    }
}
