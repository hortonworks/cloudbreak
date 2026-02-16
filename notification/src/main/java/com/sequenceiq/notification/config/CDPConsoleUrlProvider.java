package com.sequenceiq.notification.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.type.CdpResourceType;

@Service
public class CDPConsoleUrlProvider {

    @Value("${cdp.console.url:}")
    private String url;

    public String getClusterUrl(CdpResourceType type, String environmentName, String resourceName) {
        return switch (type) {
            case DATALAKE -> getDataLakeUrl(environmentName, resourceName);
            case DATAHUB -> getDataHubUrl(resourceName);
            case ENVIRONMENT -> getEnvironmentUrl(environmentName);
            default -> throw new UnsupportedOperationException("Unsupported resource type: " + type);
        };
    }

    public String getDataHubUrl(String datahubName) {
        return String.format("%s/workloads/details/%s/events", url, datahubName);
    }

    public String getDataLakeUrl(String environmentName, String datalakeName) {
        return String.format("%s/environments/details/%s/datalake/%s/events", url, environmentName, datalakeName);
    }

    public String getEnvironmentUrl(String environmentName) {
        return String.format("%s/environments/details/%s/summary", url, environmentName);
    }

}
