package com.sequenceiq.cloudbreak.converter

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.fasterxml.jackson.databind.node.TextNode
import com.sequenceiq.cloudbreak.controller.json.JsonHelper
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse

@Component
class BlueprintToJsonConverter : AbstractConversionServiceAwareConverter<Blueprint, BlueprintResponse>() {

    @Inject
    private val jsonHelper: JsonHelper? = null

    override fun convert(entity: Blueprint): BlueprintResponse {
        val blueprintJson = BlueprintResponse()
        blueprintJson.id = entity.id.toString()
        blueprintJson.blueprintName = entity.blueprintName
        blueprintJson.name = entity.name
        blueprintJson.isPublicInAccount = entity.isPublicInAccount
        blueprintJson.description = if (entity.description == null) "" else entity.description
        blueprintJson.hostGroupCount = entity.hostGroupCount
        try {
            blueprintJson.setAmbariBlueprint(jsonHelper!!.createJsonFromString(entity.blueprintText))
        } catch (e: Exception) {
            LOGGER.error("Blueprint cannot be converted to JSON.", e)
            blueprintJson.setAmbariBlueprint(TextNode(e.message))
        }

        return blueprintJson
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BlueprintToJsonConverter::class.java)
    }
}
