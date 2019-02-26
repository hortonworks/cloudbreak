package com.sequenceiq.cloudbreak.template.views;

import com.sequenceiq.cloudbreak.template.model.HdfConfigs;

public class HdfConfigView {

    private final String nodeEntities;

    private final String registryNodeEntities;

    private final String nodeUserEntities;

    private final String proxyHosts;

    public HdfConfigView(HdfConfigs hdfConfigs) {
        nodeEntities = hdfConfigs.getNodeEntities();
        proxyHosts = hdfConfigs.getProxyHosts().orElse(null);
        registryNodeEntities = hdfConfigs.getRegistryNodeEntities();
        nodeUserEntities = hdfConfigs.getNodeUserEntities();
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
