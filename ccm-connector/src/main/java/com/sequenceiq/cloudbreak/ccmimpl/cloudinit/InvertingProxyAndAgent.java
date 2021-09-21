package com.sequenceiq.cloudbreak.ccmimpl.cloudinit;

import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxy;
import com.cloudera.thunderhead.service.clusterconnectivitymanagementv2.ClusterConnectivityManagementV2Proto.InvertingProxyAgent;

class InvertingProxyAndAgent {

    private final InvertingProxy invertingProxy;

    private final InvertingProxyAgent invertingProxyAgent;

    InvertingProxyAndAgent(InvertingProxy invertingProxy, InvertingProxyAgent invertingProxyAgent) {
        this.invertingProxy = invertingProxy;
        this.invertingProxyAgent = invertingProxyAgent;
    }

    public InvertingProxy getInvertingProxy() {
        return invertingProxy;
    }

    public InvertingProxyAgent getInvertingProxyAgent() {
        return invertingProxyAgent;
    }

    @Override
    public String toString() {
        return "InvertingProxyAndAgent{" +
                "invertingProxy=" + invertingProxy +
                ", invertingProxyAgent=" + invertingProxyAgent +
                '}';
    }
}
