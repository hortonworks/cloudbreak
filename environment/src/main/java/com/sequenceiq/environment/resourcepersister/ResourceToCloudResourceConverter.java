package com.sequenceiq.environment.resourcepersister;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@Component
public class ResourceToCloudResourceConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceToCloudResourceConverter.class);

    public CloudResource convert(Resource resource) {
        return CloudResource.builder()
                .withType(resource.getResourceType())
                .withName(resource.getResourceName())
                .withReference(resource.getResourceReference())
                .withStatus(resource.getResourceStatus())
                .build();
    }
}
