package com.sequenceiq.environment.util;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;

@Component
public class ResourceDefinitionRequestFactory {
    public ResourceDefinitionRequest createResourceDefinitionRequest(CloudPlatformVariant cloudPlatformVariant, String resource) {
        return new ResourceDefinitionRequest(cloudPlatformVariant, resource);
    }
}
