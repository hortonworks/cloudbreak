package com.sequenceiq.cloudbreak.converter.spi

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter
import com.sequenceiq.cloudbreak.domain.Resource

@Component
class CloudResourceToResourceConverter : AbstractConversionServiceAwareConverter<CloudResource, Resource>() {
    override fun convert(source: CloudResource): Resource {
        val domainResource = Resource()
        domainResource.resourceType = source.type
        domainResource.resourceName = source.name
        domainResource.resourceReference = source.reference
        domainResource.resourceStatus = source.status
        return domainResource
    }
}
