package com.sequenceiq.cloudbreak.clusterdefinition;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ServiceConfig that = (ServiceConfig) o;

        return new EqualsBuilder()
                .append(serviceName, that.serviceName)
                .append(relatedServices, that.relatedServices)
                .append(globalConfig, that.globalConfig)
                .append(hostGroupConfig, that.hostGroupConfig)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(serviceName)
                .append(relatedServices)
                .append(globalConfig)
                .append(hostGroupConfig)
                .toHashCode();
    }
}
