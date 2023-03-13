package com.sequenceiq.redbeams.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.redbeams.domain.stack.DBResource;

@Component
public class DBResourceToCloudResourceConverter {

    public CloudResource convert(DBResource resource) {

        return CloudResource.builder()
                .withType(resource.getResourceType())
                .withName(resource.getResourceName())
                .withReference(resource.getResourceReference())
                .withStatus(resource.getResourceStatus())
                .build();
    }
}
