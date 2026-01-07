package com.sequenceiq.cloudbreak.cluster.api;

import java.util.Map;

import com.sequenceiq.cloudbreak.cluster.model.ServiceLocationMap;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

public interface ClusterPreCreationApi {

    String getCloudbreakClusterUserName(Cluster cluster);

    String getCloudbreakClusterPassword(Cluster cluster);

    String getDataplaneClusterUserName(Cluster cluster);

    String getDataplaneClusterPassword(Cluster cluster);

    String getClusterUserProvidedPassword(Cluster cluster);

    String getCertPath();

    String getKeystorePath();

    String getKeystorePassword();

    String getMasterKey(Cluster cluster);

    Map<String, Integer> getServicePorts(Blueprint blueprint, boolean tls);

    Map<String, String> getServiceProtocols(Blueprint blueprint, boolean tls);

    ServiceLocationMap getServiceLocations();
}
