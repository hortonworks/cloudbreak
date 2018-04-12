package com.sequenceiq.cloudbreak.converter.spi;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import org.springframework.stereotype.Component;

@Component
public class CloudResourceToResourceConverter extends AbstractConversionServiceAwareConverter<CloudResource, Resource> {
    @Override
    public Resource convert(CloudResource source) {
        Resource domainResource = new Resource();
        domainResource.setResourceType(source.getType());
        domainResource.setResourceName(source.getName());
        domainResource.setResourceReference(source.getReference());
        domainResource.setResourceStatus(source.getStatus());
        domainResource.setInstanceGroup(source.getGroup());
        return domainResource;
    }
}
