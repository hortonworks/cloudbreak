package com.sequenceiq.redbeams.converter.stack;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource.Builder;
import com.sequenceiq.redbeams.domain.stack.DBResource;

@Component
public class DbResourceTocloudResourceConverter {

    public CloudResource convert(DBResource source) {
        return new Builder()
                .withType(source.getResourceType())
                .withName(source.getResourceName())
                .withReference(source.getResourceReference())
                .withStatus(source.getResourceStatus())
                .build();
    }
}
