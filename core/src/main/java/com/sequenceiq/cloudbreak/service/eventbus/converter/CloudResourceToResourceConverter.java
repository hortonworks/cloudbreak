package com.sequenceiq.cloudbreak.service.eventbus.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Resource;

@Component
public class CloudResourceToResourceConverter extends AbstractConversionServiceAwareConverter<CloudResource, Resource> {
    @Override
    public Resource convert(CloudResource source) {
        Resource domainResource = new Resource();
        domainResource.setResourceType(source.getType());
        domainResource.setResourceName(source.getReference());
        return domainResource;
    }
}
