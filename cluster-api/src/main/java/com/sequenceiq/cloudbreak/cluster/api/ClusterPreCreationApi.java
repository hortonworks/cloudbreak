package com.sequenceiq.cloudbreak.cluster.api;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public interface ClusterPreCreationApi {
    boolean isVdfReady(AmbariRepo ambariRepo);

    boolean isLdapAndSSOReady(AmbariRepo ambariRepo);

    String getCloudbreakClusterUserName(Cluster cluster);

    String getCloudbreakClusterPassword(Cluster cluster);

    String getDataplaneClusterUserName(Cluster cluster);

    String getDataplaneClusterPassword(Cluster cluster);

    String getClusterUserProvidedPassword(Cluster cluster);

    String getCertPath();

    String getKeystorePath();

    String getKeystorePassword();

    String getMasterKey(Cluster cluster);
}
