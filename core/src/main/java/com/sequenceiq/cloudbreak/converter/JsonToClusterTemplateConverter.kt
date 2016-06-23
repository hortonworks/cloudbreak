package com.sequenceiq.cloudbreak.converter

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.fasterxml.jackson.core.JsonProcessingException
import com.sequenceiq.cloudbreak.api.model.ClusterTemplateRequest
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.json.JsonHelper
import com.sequenceiq.cloudbreak.domain.ClusterTemplate
import com.sequenceiq.cloudbreak.domain.json.Json

@Component
class JsonToClusterTemplateConverter : AbstractConversionServiceAwareConverter<ClusterTemplateRequest, ClusterTemplate>() {

    @Inject
    private val jsonHelper: JsonHelper? = null

    override fun convert(json: ClusterTemplateRequest): ClusterTemplate {
        val clusterTemplate = ClusterTemplate()
        clusterTemplate.name = json.name
        try {
            clusterTemplate.template = Json(json.template)
        } catch (e: JsonProcessingException) {
            LOGGER.error("Cloudtemplate cannot be converted to JSON: " + json.template, e)
            throw BadRequestException("Cloudtemplate cannot be converted to JSON", e)
        }

        clusterTemplate.type = json.type
        return clusterTemplate
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(JsonToClusterTemplateConverter::class.java)
    }
}
