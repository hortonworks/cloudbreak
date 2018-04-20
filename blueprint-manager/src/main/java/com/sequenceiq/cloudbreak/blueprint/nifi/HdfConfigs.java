package com.sequenceiq.cloudbreak.blueprint.nifi;

import java.util.Optional;

public class HdfConfigs {

    private String nodeEntities;

    private String registryNodeEntities;

    private Optional<String> proxyHosts;

    public HdfConfigs(String nodeEntities, String registryNodeEntities, Optional<String> proxyHosts) {
        this.nodeEntities = nodeEntities;
        this.proxyHosts = proxyHosts;
        this.registryNodeEntities = registryNodeEntities;
    }

    public String getNodeEntities() {
        return nodeEntities;
    }

    public String getRegistryNodeEntities() {
        return registryNodeEntities;
    }

    public Optional<String> getProxyHosts() {
        return proxyHosts;
    }
}
