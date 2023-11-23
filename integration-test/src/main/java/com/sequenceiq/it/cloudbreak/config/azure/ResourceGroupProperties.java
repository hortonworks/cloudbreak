package com.sequenceiq.it.cloudbreak.config.azure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceGroupProperties {

    @Value("${integrationtest.azure.resourcegroup.usage}")
    private String resourceGroupUsage;

    @Value("${integrationtest.azure.resourcegroup.name}")
    private String resourceGroupName;

    public String getResourceGroupUsage() {
        return resourceGroupUsage;
    }

    public void setResourceGroupUsage(String resourceGroupUsage) {
        this.resourceGroupUsage = resourceGroupUsage;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }
}