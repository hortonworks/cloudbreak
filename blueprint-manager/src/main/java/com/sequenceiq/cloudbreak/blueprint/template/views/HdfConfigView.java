package com.sequenceiq.cloudbreak.blueprint.template.views;

import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigs;

public class HdfConfigView {

    private String nodeEntities;

    private String registryNodeEntities;

    private String proxyHosts;

    public HdfConfigView(HdfConfigs hdfConfigs) {
        this.nodeEntities = hdfConfigs.getNodeEntities();
        this.proxyHosts = hdfConfigs.getProxyHosts().orElse(null);
        this.registryNodeEntities = hdfConfigs.getRegistryNodeEntities();
    }

    public String getNodeEntities() {
        return nodeEntities;
    }

    public String getRegistryNodeEntities() {
        return registryNodeEntities;
    }

    public String getProxyHosts() {
        return proxyHosts;
    }
}
