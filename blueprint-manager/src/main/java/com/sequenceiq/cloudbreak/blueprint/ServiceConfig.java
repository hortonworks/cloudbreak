package com.sequenceiq.cloudbreak.blueprint;

import java.util.List;
import java.util.Map;

public class ServiceConfig {

    private final String serviceName;

    private final List<String> relatedServices;

    private final Map<String, List<ConfigProperty>> globalConfig;

    private final Map<String, List<ConfigProperty>> hostGroupConfig;

    public ServiceConfig(String serviceName, List<String> relatedServices,
            Map<String, List<ConfigProperty>> globalConfig, Map<String, List<ConfigProperty>> hostGroupConfig) {
        this.serviceName = serviceName;
        this.relatedServices = relatedServices;
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

    public List<String> getRelatedServices() {
        return relatedServices;
    }
}
