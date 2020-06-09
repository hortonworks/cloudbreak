package com.sequenceiq.it.cloudbreak.config.azure;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.ResourceGroupTest;

@Configuration
public class ResourceGroupProperties {

    @Value("${integrationtest.azure.resourcegroup.usage}")
    private String resourceGroupUsage;

    @Value("${integrationtest.azure.resourcegroup.name}")
    private String resourceGroupName;

    @Inject
    private TestParameter testParameter;

    @PostConstruct
    private void init() {
        testParameter.put(ResourceGroupTest.AZURE_RESOURCE_GROUP_USAGE, resourceGroupUsage);
        testParameter.put(ResourceGroupTest.AZURE_RESOURCE_GROUP_NAME, resourceGroupName);
    }
}