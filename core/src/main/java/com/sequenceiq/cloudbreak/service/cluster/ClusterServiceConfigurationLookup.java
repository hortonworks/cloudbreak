package com.sequenceiq.cloudbreak.service.cluster;

public class ClusterServiceConfigurationLookup {

    private String serviceName;

    private String configName;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getConfigName() {
        return configName;
    }

    public void setConfigName(String configName) {
        this.configName = configName;
    }

    @Override
    public String toString() {
        return "ClusterServiceConfigurationLookup{" +
                "serviceName='" + serviceName + '\'' +
                ", configName='" + configName + '\'' +
                '}';
    }
}
