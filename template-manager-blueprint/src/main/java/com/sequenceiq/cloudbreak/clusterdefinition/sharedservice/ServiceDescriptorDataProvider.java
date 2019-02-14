package com.sequenceiq.cloudbreak.clusterdefinition.sharedservice;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;

@Service
public class ServiceDescriptorDataProvider {

    public static final String RANGER = "RANGER";

    public static final String RANGER_ADMIN = "RANGER_ADMIN";

    public static final String RANGER_PASSWORD = "ranger.admin.password";

    public static final String RANGER_PORT = "ranger.service.http.port";

    public String getHostForComponentInService(DatalakeResources datalakeResources, String service, String serviceComponent) {
        if (datalakeResources.getServiceDescriptorMap().containsKey(service)) {
            ServiceDescriptor serviceDescriptor = datalakeResources.getServiceDescriptorMap().get(service);
            Map<String, Object> componentHostMap = serviceDescriptor.getComponentsHosts().getMap();
            return String.valueOf(componentHostMap.get(serviceComponent));
        } else {
            return null;
        }
    }

    public String getRangerAdminHost(DatalakeResources datalakeResources) {
        return getHostForComponentInService(datalakeResources, RANGER, RANGER_ADMIN);
    }

    public String getRangerPort(Map<String, ServiceDescriptor> serviceDescriptorMap, String defaultPort) {
        if (serviceDescriptorMap.containsKey(RANGER)) {
            ServiceDescriptor serviceDescriptor = serviceDescriptorMap.get(RANGER);
            Map<String, Object> params = serviceDescriptor.getBlueprintParams().getMap();
            return String.valueOf(params.getOrDefault(RANGER_PORT, defaultPort));
        }
        return defaultPort;
    }

    public String getServiceRelatedSecret(Map<String, ServiceDescriptor> serviceDescriptorMap, String service, String secretKey) {
        if (serviceDescriptorMap.containsKey(service)) {
            ServiceDescriptor serviceDescriptor = serviceDescriptorMap.get(service);
            Map<String, Object> secretMap = serviceDescriptor.getBlueprintSecretParams().getMap();
            if (secretMap.containsKey(secretKey)) {
                return String.valueOf(secretMap.get(secretKey));
            }
        }
        return null;
    }

    public String getServiceRelatedSecret(DatalakeResources datalakeResources, String service, String secretKey) {
        return getServiceRelatedSecret(datalakeResources.getServiceDescriptorMap(), service, secretKey);
    }

    public String getRangerAdminPassword(DatalakeResources datalakeResources) {
        return getServiceRelatedSecret(datalakeResources, RANGER, RANGER_PASSWORD);
    }
}
