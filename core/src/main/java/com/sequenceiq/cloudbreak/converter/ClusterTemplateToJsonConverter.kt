package com.sequenceiq.cloudbreak.converter

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.ClusterTemplateResponse
import com.sequenceiq.cloudbreak.domain.ClusterTemplate

@Component
class ClusterTemplateToJsonConverter : AbstractConversionServiceAwareConverter<ClusterTemplate, ClusterTemplateResponse>() {

    override fun convert(json: ClusterTemplate): ClusterTemplateResponse {
        val clusterTemplateResponse = ClusterTemplateResponse()
        clusterTemplateResponse.name = json.name
        clusterTemplateResponse.template = json.template.value
        clusterTemplateResponse.type = json.type
        clusterTemplateResponse.id = json.id
        return clusterTemplateResponse
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ClusterTemplateToJsonConverter::class.java)
    }
}
