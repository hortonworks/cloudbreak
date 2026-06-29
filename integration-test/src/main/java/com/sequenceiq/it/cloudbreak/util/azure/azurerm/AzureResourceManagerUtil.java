package com.sequenceiq.it.cloudbreak.util.azure.azurerm;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.util.azure.AzureResources;
import com.sequenceiq.it.cloudbreak.util.azure.azurerm.action.AzureResourceManagerAction;

@Component
public class AzureResourceManagerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureResourceManagerUtil.class);

    @Inject
    private AzureResourceManagerAction azureResourceManagerAction;

    private AzureResourceManagerUtil() {
    }

    public Map<String, Map<String, String>> getAllResourcesAndTagsForEnvironment(String envCrn) {
        LOGGER.info("TAG VALIDATION: Getting tags for all resource types for env CRN: {}", envCrn);
        List<AzureResources> allResources = Arrays.asList(AzureResources.values());
        Map<String, Map<String, String>> result =
                azureResourceManagerAction.getResourcesAndTagsByEnvironmentCrnAndResourceTypes(envCrn, allResources);
        Arrays.stream(AzureResources.values()).forEach(resourceType -> {
            long count = countResourceIdsMatchingType(result.keySet(), resourceType);
            LOGGER.info("TAG VALIDATION: Got {} resources of type {} ({}) for the Environment",
                    count, resourceType.name(), resourceType.getResourceType());
        });
        LOGGER.info("TAG VALIDATION: Got {} total resources for env CRN: {}", result.size(), envCrn);
        return result;
    }

    private long countResourceIdsMatchingType(Set<String> resourceIds, AzureResources resourceType) {
        String resourceTypePath = resourceType.getResourceType().toLowerCase();
        return resourceIds.stream()
                .filter(id -> id.toLowerCase().contains("/providers/" + resourceTypePath + "/"))
                .count();
    }
}