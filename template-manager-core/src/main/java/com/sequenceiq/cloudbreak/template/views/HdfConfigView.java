package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.template.model.HdfConfigs;

public class HdfConfigView {

    private String nodeEntities;

    private String registryNodeEntities;

    private String nodeUserEntities;

    private String proxyHosts;

    public HdfConfigView(HdfConfigs hdfConfigs) {
        this.nodeEntities = hdfConfigs.getNodeEntities();
        this.proxyHosts = hdfConfigs.getProxyHosts().orElse(null);
        this.registryNodeEntities = hdfConfigs.getRegistryNodeEntities();
        this.nodeUserEntities = hdfConfigs.getNodeUserEntities();
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

    public String getProxyHosts() {
        return proxyHosts;
    }
}
