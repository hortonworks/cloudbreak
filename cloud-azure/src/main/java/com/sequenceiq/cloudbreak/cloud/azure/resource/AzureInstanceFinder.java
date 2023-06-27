package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static com.sequenceiq.common.api.type.ResourceType.AZURE_INSTANCE;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Component
public class AzureInstanceFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceFinder.class);

    public CloudResource getInstanceCloudResource(long privateId, List<CloudResource> computeResources) {
        LOGGER.info("Find instance for private id {} from resources {}", privateId, computeResources);
        return computeResources.stream()
                .filter(cr -> cr.getType().equals(AZURE_INSTANCE))
                .findFirst()
                .orElseThrow(() -> new AzureResourceException("Instance resource not found for instance with private ID: " + privateId));
    }
}
