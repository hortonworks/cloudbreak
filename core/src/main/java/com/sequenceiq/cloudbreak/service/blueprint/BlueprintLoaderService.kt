package com.sequenceiq.cloudbreak.service.blueprint

import java.util.HashSet

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.controller.json.JsonHelper
import com.sequenceiq.cloudbreak.domain.Blueprint
import com.sequenceiq.cloudbreak.domain.CbUser
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest
import com.sequenceiq.cloudbreak.repository.BlueprintRepository
import com.sequenceiq.cloudbreak.util.FileReaderUtils

@Component
class BlueprintLoaderService {

    @Value("#{'${cb.blueprint.defaults:}'.split(';')}")
    private val blueprintArray: List<String>? = null

    @Inject
    @Qualifier("conversionService")
    private val conversionService: ConversionService? = null

    @Inject
    private val blueprintRepository: BlueprintRepository? = null

    @Inject
    private val jsonHelper: JsonHelper? = null

    fun loadBlueprints(user: CbUser): Set<Blueprint> {
        val blueprints = HashSet<Blueprint>()
        val blueprintNames = getDefaultBlueprintNames(user)
        for (blueprintStrings in blueprintArray!!) {
            val split = blueprintStrings.split("=".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            if (!blueprintStrings.isEmpty() && (split.size == 2 || split.size == 1) && !blueprintNames.contains(blueprintStrings)
                    && !split[0].isEmpty()) {
                LOGGER.info("Adding default blueprint '{}' for user '{}'", blueprintStrings, user.username)
                try {
                    val blueprintJson = BlueprintRequest()
                    blueprintJson.name = split[0]
                    blueprintJson.description = split[0]
                    blueprintJson.setAmbariBlueprint(jsonHelper!!.createJsonFromString(
                            FileReaderUtils.readFileFromClasspath(String.format("defaults/blueprints/%s.bp", if (split.size == 2) split[1] else split[0]))))
                    val bp = conversionService!!.convert<Blueprint>(blueprintJson, Blueprint::class.java)
                    bp.owner = user.userId
                    bp.account = user.account
                    bp.isPublicInAccount = true
                    bp.status = ResourceStatus.DEFAULT
                    blueprintRepository!!.save(bp)
                    blueprints.add(bp)
                } catch (e: Exception) {
                    LOGGER.error("Blueprint is not available for '{}' user.", e, user)
                }

            }
        }
        return blueprints
    }

    private fun getDefaultBlueprintNames(user: CbUser): Set<String> {
        val defaultBpNames = HashSet<String>()
        val defaultBlueprints = blueprintRepository!!.findAllDefaultInAccount(user.account)
        for (defaultBlueprint in defaultBlueprints) {
            defaultBpNames.add(defaultBlueprint.name)
        }
        return defaultBpNames
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(BlueprintLoaderService::class.java)
    }

}
