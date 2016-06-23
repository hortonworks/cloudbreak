package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.api.model.TemplateResponse

@Component
class TemplateToJsonConverter : AbstractConversionServiceAwareConverter<Template, TemplateResponse>() {
    override fun convert(source: Template): TemplateResponse {
        val templateJson = TemplateResponse()
        templateJson.id = source.id
        templateJson.name = source.name
        templateJson.volumeCount = source.volumeCount
        templateJson.volumeSize = source.volumeSize
        templateJson.isPublicInAccount = source.isPublicInAccount
        templateJson.instanceType = source.instanceType
        templateJson.volumeType = source.volumeType
        val attributes = source.attributes
        if (attributes != null) {
            templateJson.parameters = attributes.map
        }
        templateJson.cloudPlatform = source.cloudPlatform()
        templateJson.description = if (source.description == null) "" else source.description
        if (source.topology != null) {
            templateJson.topologyId = source.topology.id
        }
        return templateJson
    }
}
