package com.sequenceiq.cloudbreak.service.sharedservice;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptorDefinition;

@Component
@ConfigurationProperties
public class ServiceDescriptorDefinitionProvider {
    public static final String RANGER_SERVICE = "RANGER";

    public static final String RANGER_ADMIN_COMPONENT = "RANGER_ADMIN";

    public static final String RANGER_ADMIN_PWD_KEY = "ranger.admin.password";

    public static final String RANGER_HTTPPORT_KEY = "ranger.service.http.port";

    public static final String YARN_RESOURCEMANAGER_WEBAPP_ADDRESS = "yarn.resourcemanager.webapp.address";

    private Map<String, ServiceDescriptorDefinition> serviceDescriptorDefinitionMap;

    public Map<String, ServiceDescriptorDefinition> getServiceDescriptorDefinitionMap() {
        return serviceDescriptorDefinitionMap;
    }

    public void setServiceDescriptorDefinitionMap(Map<String, ServiceDescriptorDefinition> serviceDescriptorDefinitionMap) {
        this.serviceDescriptorDefinitionMap = serviceDescriptorDefinitionMap;
    }
}
