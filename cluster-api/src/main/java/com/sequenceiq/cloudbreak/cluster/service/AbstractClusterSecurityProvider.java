package com.sequenceiq.cloudbreak.cluster.service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public abstract class AbstractClusterSecurityProvider {
    public String getCloudbreakClusterUserName(Cluster cluster) {
        return cluster.getCloudbreakAmbariUser();
    }

    public String getCloudbreakClusterPassword(Cluster cluster) {
        return cluster.getCloudbreakAmbariPassword();
    }

    public String getDataplaneClusterUserName(Cluster cluster) {
        return cluster.getDpAmbariUser();
    }

    public String getDataplaneClusterPassword(Cluster cluster) {
        return cluster.getDpAmbariPassword();
    }

    public String getClusterUserProvidedPassword(Cluster cluster) {
        return cluster.getPassword();
    }

    public abstract String getCertPath();

    public abstract String getKeystorePath();

    public abstract String getKeystorePassword();

    public abstract String getMasterKey(Cluster cluster);
}
