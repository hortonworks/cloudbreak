package com.sequenceiq.cloudbreak.service.eventbus.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Resource;

@Component
public class ResourceToCloudResourceConverter extends AbstractConversionServiceAwareConverter<Resource, CloudResource> {
    @Override
    public CloudResource convert(Resource source) {
        return new CloudResource(source.getResourceType(), source.getResourceName(), source.getResourceName());
    }
}
