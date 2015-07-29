package com.sequenceiq.cloudbreak.converter.spi;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Resource;

@Component
public class ResourceToCloudResourceConverter extends AbstractConversionServiceAwareConverter<Resource, CloudResource> {

    @Override
    public CloudResource convert(Resource resource) {
        return new CloudResource(resource.getResourceType(), resource.getStack().getName(), resource.getResourceName());
    }


}
