package com.sequenceiq.cloudbreak.converter.spi

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter
import com.sequenceiq.cloudbreak.domain.Resource

@Component
class ResourceToCloudResourceConverter : AbstractConversionServiceAwareConverter<Resource, CloudResource>() {
    override fun convert(resource: Resource): CloudResource {
        return CloudResource.Builder().type(resource.resourceType).name(resource.resourceName).reference(resource.resourceReference).status(resource.resourceStatus).build()
    }
}
