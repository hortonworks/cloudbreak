package com.sequenceiq.environment.resourcepersister;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;

@Component
public class ResourceToCloudResourceConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceToCloudResourceConverter.class);

    public CloudResource convert(Resource resource) {
        return new Builder()
                .withType(resource.getResourceType())
                .withName(resource.getResourceName())
                .withReference(resource.getResourceReference())
                .withStatus(resource.getResourceStatus())
                .build();
    }
}
