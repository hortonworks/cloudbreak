package com.sequenceiq.cloudbreak.converter

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.controller.BadRequestException
import com.sequenceiq.cloudbreak.controller.json.JsonHelper
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest
import com.sequenceiq.cloudbreak.util.JsonUtil

@Component
class JsonToBlueprintConverter : AbstractConversionServiceAwareConverter<BlueprintRequest, Blueprint>() {

    @Inject
    private val jsonHelper: JsonHelper? = null

    override fun convert(json: BlueprintRequest): Blueprint {
        val blueprint = Blueprint()
        if (json.url != null && !json.url.isEmpty()) {
            val sourceUrl = json.url.trim { it <= ' ' }
            try {
                val urlText = readUrl(sourceUrl)
                jsonHelper!!.createJsonFromString(urlText)
                blueprint.blueprintText = urlText
            } catch (e: Exception) {
                throw BadRequestException("Cannot download ambari blueprint from: " + sourceUrl, e)
            }

        } else {
            blueprint.blueprintText = json.ambariBlueprint
        }
        validateBlueprint(blueprint.blueprintText)
        blueprint.name = json.name
        blueprint.description = json.description
        blueprint.status = ResourceStatus.USER_MANAGED
        try {
            val root = JsonUtil.readTree(blueprint.blueprintText)
            blueprint.blueprintName = getBlueprintName(root)
            blueprint.hostGroupCount = countHostGroups(root)
        } catch (e: IOException) {
            throw BadRequestException("Invalid Blueprint: Failed to parse JSON.", e)
        }

        return blueprint
    }


    fun convert(name: String, blueprintText: String, publicInAccount: Boolean): Blueprint {
        val blueprint = Blueprint()
        blueprint.name = name
        blueprint.blueprintText = blueprintText
        blueprint.isPublicInAccount = publicInAccount
        validateBlueprint(blueprint.blueprintText)
        try {
            val root = JsonUtil.readTree(blueprint.blueprintText)
            blueprint.blueprintName = getBlueprintName(root)
            blueprint.hostGroupCount = countHostGroups(root)
        } catch (e: IOException) {
            throw BadRequestException("Invalid Blueprint: Failed to parse JSON.", e)
        }

        return blueprint
    }

    private fun getBlueprintName(root: JsonNode): String {
        return root.get("Blueprints").get("blueprint_name").asText()
    }

    private fun countHostGroups(root: JsonNode): Int {
        var hostGroupCount = 0
        val hostGroups = root.get("host_groups").elements()
        while (hostGroups.hasNext()) {
            hostGroups.next()
            hostGroupCount++
        }
        return hostGroupCount
    }

    @Throws(IOException::class)
    private fun readUrl(url: String): String {
        var url = url
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url
        }
        val `in` = BufferedReader(InputStreamReader(URL(url).openStream()))
        var str: String
        val sb = StringBuffer()
        while ((str = `in`.readLine()) != null) {
            sb.append(str)
        }
        `in`.close()
        return sb.toString()
    }

    private fun validateBlueprint(blueprintText: String) {
        try {
            val root = JsonUtil.readTree(blueprintText)
            hasBlueprintInBlueprint(root)
            hasBlueprintNameInBlueprint(root)
            validateHostGroups(root)
        } catch (e: IOException) {
            throw BadRequestException("Invalid Blueprint: Failed to parse JSON.", e)
        }

    }

    private fun validateHostGroups(root: JsonNode) {
        val hostGroups = root.path("host_groups")
        if (hostGroups.isMissingNode || !hostGroups.isArray || hostGroups.size() == 0) {
            throw BadRequestException("Invalid blueprint: 'host_groups' node is missing from JSON or is not an array or empty.")
        }
        for (hostGroup in hostGroups) {
            val hostGroupName = hostGroup.path("name")
            if (hostGroupName.isMissingNode || !hostGroupName.isTextual || hostGroupName.asText().isEmpty()) {
                throw BadRequestException("Invalid blueprint: one of the 'host_groups' has no name.")
            }
            validateComponentsInHostgroup(hostGroup, hostGroupName.asText())
        }
    }

    private fun validateComponentsInHostgroup(hostGroup: JsonNode, hostGroupName: String) {
        val components = hostGroup.path("components")
        if (components.isMissingNode || !components.isArray || components.size() == 0) {
            throw BadRequestException(
                    String.format("Invalid blueprint: '%s' hostgroup's 'components' node is missing from JSON or is not an array or empty.", hostGroupName))
        }
        for (component in components) {
            val componentName = component.path("name")
            if (componentName.isMissingNode || !componentName.isTextual || componentName.asText().isEmpty()) {
                throw BadRequestException(String.format("Invalid blueprint: one fo the 'components' has no name in '%s' hostgroup.", hostGroupName))
            }
        }
    }

    private fun hasBlueprintNameInBlueprint(root: JsonNode) {
        if (root.path("Blueprints").path("blueprint_name").isMissingNode) {
            throw BadRequestException("Invalid blueprint: 'blueprint_name' under 'Blueprints' is missing from JSON.")
        }
    }

    private fun hasBlueprintInBlueprint(root: JsonNode) {
        if (root.path("Blueprints").isMissingNode) {
            throw BadRequestException("Invalid blueprint: 'Blueprints' node is missing from JSON.")
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(JsonToBlueprintConverter::class.java)
    }
}
