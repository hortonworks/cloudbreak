package com.sequenceiq.cloudbreak.cluster.service;

import com.sequenceiq.cloudbreak.view.ClusterView;

public abstract class AbstractClusterSecurityProvider {
    public String getCloudbreakClusterUserName(ClusterView cluster) {
        return cluster.getCloudbreakAmbariUser();
    }

    public String getCloudbreakClusterPassword(ClusterView cluster) {
        return cluster.getCloudbreakAmbariPassword();
    }

    public String getDataplaneClusterUserName(ClusterView cluster) {
        return cluster.getDpAmbariUser();
    }

    public String getDataplaneClusterPassword(ClusterView cluster) {
        return cluster.getDpAmbariPassword();
    }

    public String getClusterUserProvidedPassword(ClusterView cluster) {
        return cluster.getPassword();
    }

    public abstract String getCertPath();

    public abstract String getKeystorePath();

    public abstract String getKeystorePassword();

    public abstract String getMasterKey(ClusterView cluster);
}
