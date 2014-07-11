package com.sequenceiq.periscope.registry;

import org.apache.hadoop.yarn.client.api.YarnClient;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.service.configuration.AmbariConfigurationService;

public class ClusterRegistration {

    private final String clusterId;
    private final Ambari ambari;
    private final AmbariClient ambariClient;
    private final AmbariConfigurationService configurationService;
    private final YarnClient yarnClient;

    public ClusterRegistration(String clusterId, Ambari ambari) {
        this.clusterId = clusterId;
        this.ambari = ambari;
        this.ambariClient = new AmbariClient(ambari.getHost(), ambari.getPort(), ambari.getUser(), ambari.getPass());
        this.configurationService = new AmbariConfigurationService(ambariClient);
        this.yarnClient = YarnClient.createYarnClient();
        this.yarnClient.init(configurationService.getConfiguration());
        this.yarnClient.start();
    }

    public String getClusterId() {
        return clusterId;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
    }

    public AmbariConfigurationService getConfigurationService() {
        return configurationService;
    }

    public YarnClient getYarnClient() {
        return yarnClient;
    }

    public String getHost() {
        return ambari.getHost();
    }

    public String getPort() {
        return ambari.getPort();
    }

    public String getUser() {
        return ambari.getUser();
    }

    public String getPass() {
        return ambari.getPass();
    }
}
