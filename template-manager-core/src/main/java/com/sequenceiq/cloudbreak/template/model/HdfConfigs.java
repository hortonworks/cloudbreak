package com.sequenceiq.cloudbreak.template.model;

import java.util.Optional;

public class HdfConfigs {

    private String nodeEntities;

    private String registryNodeEntities;

    private String nodeUserEntities;

    private Optional<String> proxyHosts;

    public HdfConfigs(String nodeEntities, String registryNodeEntities, String nodeUserEntities, Optional<String> proxyHosts) {
        this.nodeEntities = nodeEntities;
        this.proxyHosts = proxyHosts;
        this.registryNodeEntities = registryNodeEntities;
        this.nodeUserEntities = nodeUserEntities;
    }

    public String getNodeEntities() {
        return nodeEntities;
    }

    public String getRegistryNodeEntities() {
        return registryNodeEntities;
    }

    public String getNodeUserEntities() {
        return nodeUserEntities;
    }

    public Optional<String> getProxyHosts() {
        return proxyHosts;
    }
}
