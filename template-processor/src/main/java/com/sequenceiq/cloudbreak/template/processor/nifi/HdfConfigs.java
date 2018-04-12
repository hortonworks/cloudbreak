package com.sequenceiq.cloudbreak.template.processor.nifi;

import java.util.Optional;

public class HdfConfigs {

    private String nodeEntities;

    private Optional<String> proxyHosts;

    public HdfConfigs(String nodeEntities, Optional<String> proxyHosts) {
        this.nodeEntities = nodeEntities;
        this.proxyHosts = proxyHosts;
    }

    public String getNodeEntities() {
        return nodeEntities;
    }

    public Optional<String> getProxyHosts() {
        return proxyHosts;
    }
}
