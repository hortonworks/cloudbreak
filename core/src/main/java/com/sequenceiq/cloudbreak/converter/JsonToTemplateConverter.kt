package com.sequenceiq.cloudbreak.converter

import javax.inject.Inject

import org.springframework.stereotype.Component

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.domain.Template
import com.sequenceiq.cloudbreak.domain.json.Json
import com.sequenceiq.cloudbreak.api.model.TemplateRequest
import com.sequenceiq.cloudbreak.service.topology.TopologyService

@Component
class JsonToTemplateConverter : AbstractConversionServiceAwareConverter<TemplateRequest, Template>() {
    @Inject
    private val topologyService: TopologyService? = null

    override fun convert(source: TemplateRequest): Template {
        val template = Template()
        template.name = source.name
        template.description = source.description
        template.status = ResourceStatus.USER_MANAGED
        template.volumeCount = if (source.volumeCount == null) 0 else source.volumeCount
        template.volumeSize = if (source.volumeSize == null) 0 else source.volumeSize
        template.setCloudPlatform(source.cloudPlatform)
        template.instanceType = source.instanceType
        val volumeType = source.volumeType
        template.volumeType = volumeType ?: "HDD"
        val parameters = source.parameters
        if (parameters != null && !parameters.isEmpty()) {
            try {
                template.attributes = Json(parameters)
            } catch (e: JsonProcessingException) {
                throw BadRequestException("Invalid parameters")
            }

        }
        if (source.topologyId != null) {
            template.topology = topologyService!!.get(source.topologyId)
        }
        return template
    }
}
