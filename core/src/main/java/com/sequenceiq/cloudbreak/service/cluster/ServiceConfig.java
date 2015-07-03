package com.sequenceiq.cloudbreak.service.cluster;

import java.util.List;
import java.util.Map;

public class ServiceConfig {

    private final String serviceName;
    private final Map<String, List<ConfigProperty>> globalConfig;
    private final Map<String, List<ConfigProperty>> hostGroupConfig;

    public ServiceConfig(String serviceName, Map<String, List<ConfigProperty>> globalConfig, Map<String, List<ConfigProperty>> hostGroupConfig) {
        this.serviceName = serviceName;
        this.globalConfig = globalConfig;
        this.hostGroupConfig = hostGroupConfig;
    }

    public Map<String, List<ConfigProperty>> getGlobalConfig() {
        return globalConfig;
    }

    public Map<String, List<ConfigProperty>> getHostGroupConfig() {
        return hostGroupConfig;
    }

    public String getServiceName() {
        return serviceName;
    }
}
