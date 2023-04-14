package com.sequenceiq.cloudbreak.cluster.service;

import com.sequenceiq.cloudbreak.view.ClusterView;

public abstract class AbstractClusterSecurityProvider {

    public String getClusterUserProvidedPassword(ClusterView cluster) {
        return cluster.getPassword();
    }

    public abstract String getCertPath();

    public abstract String getKeystorePath();

    public abstract String getKeystorePassword();

    public abstract String getMasterKey(ClusterView cluster);
}
