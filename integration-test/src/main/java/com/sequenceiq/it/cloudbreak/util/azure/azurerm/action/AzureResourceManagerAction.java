package com.sequenceiq.it.cloudbreak.util.azure.azurerm.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.azure.core.http.rest.PagedIterable;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.sequenceiq.it.cloudbreak.util.azure.AzureResources;

@Component
public class AzureResourceManagerAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceManagerAction.class);

    private static final String CLOUDERA_ENVIRONMENT_RESOURCE_NAME = "Cloudera-Environment-Resource-Name";

    @Inject
    private AzureResourceManager azureResourceManager;

    public Map<String, Map<String, String>> getResourcesAndTagsByEnvironmentCrnAndResourceTypes(String envCrn, List<AzureResources> resourceTypes) {
        Map<String, Map<String, String>> resourcesAndTags = new HashMap<>();

        for (AzureResources resourceType : resourceTypes) {
            LOGGER.info("Fetching resources of type: {} for environment CRN: {}", resourceType.getResourceType(), envCrn);

            PagedIterable<GenericResource> resources = azureResourceManager.genericResources().list();

            for (GenericResource resource : resources) {
                if (resource.type().equalsIgnoreCase(resourceType.getResourceType()) &&
                        resource.tags().containsKey(CLOUDERA_ENVIRONMENT_RESOURCE_NAME) &&
                        resource.tags().get(CLOUDERA_ENVIRONMENT_RESOURCE_NAME).equals(envCrn)) {

                    resourcesAndTags.put(resource.id(), resource.tags());
                }
            }
        }

        LOGGER.info("Fetched {} resources for environment CRN: {}", resourcesAndTags.size(), envCrn);
        return resourcesAndTags;
    }
}
