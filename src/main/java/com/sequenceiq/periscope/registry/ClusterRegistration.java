package com.sequenceiq.periscope.registry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.client.api.YarnClient;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.periscope.model.Ambari;
import com.sequenceiq.periscope.service.configuration.AmbariConfigurationService;
import com.sequenceiq.periscope.service.configuration.ConfigParam;

public class ClusterRegistration {

    private final String clusterId;
    private final Ambari ambari;
    private final AmbariClient ambariClient;
    private final Configuration configuration;
    private final YarnClient yarnClient;

    public ClusterRegistration(String clusterId, Ambari ambari) throws ConnectionException {
        this.clusterId = clusterId;
        this.ambari = ambari;
        try {
            this.ambariClient = new AmbariClient(ambari.getHost(), ambari.getPort(), ambari.getUser(), ambari.getPass());
            this.configuration = AmbariConfigurationService.getConfiguration(ambariClient);
            this.yarnClient = YarnClient.createYarnClient();
            this.yarnClient.init(configuration);
            this.yarnClient.start();
        } catch (Exception e) {
            throw new ConnectionException(ambari.getHost());
        }
    }

    public String getClusterId() {
        return clusterId;
    }

    public AmbariClient getAmbariClient() {
        return ambariClient;
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

    public String getConfigValue(ConfigParam param, String defaultValue) {
        return configuration.get(param.key(), defaultValue);
    }

}
