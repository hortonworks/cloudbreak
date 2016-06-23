package com.sequenceiq.cloudbreak.converter

import java.util.stream.Collectors

import javax.inject.Inject

import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.ConstraintJson
import com.sequenceiq.cloudbreak.api.model.HostGroupJson
import com.sequenceiq.cloudbreak.api.model.HostMetadataJson
import com.sequenceiq.cloudbreak.domain.HostGroup
import com.sequenceiq.cloudbreak.domain.HostMetadata
import com.sequenceiq.cloudbreak.domain.Recipe

@Component
class HostGroupToJsonConverter : AbstractConversionServiceAwareConverter<HostGroup, HostGroupJson>() {

    @Inject
    private val conversionService: ConversionService? = null

    override fun convert(source: HostGroup): HostGroupJson {
        val hostGroupJson = HostGroupJson()
        hostGroupJson.name = source.name
        hostGroupJson.constraint = conversionService!!.convert<ConstraintJson>(source.constraint, ConstraintJson::class.java)
        hostGroupJson.recipeIds = getRecipeIds(source.recipes)
        hostGroupJson.metadata = getHostMetadata(source.hostMetadata)
        return hostGroupJson
    }

    private fun getHostMetadata(hostMetadata: Set<HostMetadata>): Set<HostMetadataJson> {
        return hostMetadata.stream().map({ metadata ->
            val hostMetadataJson = HostMetadataJson()
            hostMetadataJson.id = metadata.getId()
            hostMetadataJson.groupName = metadata.getHostGroup().getName()
            hostMetadataJson.name = metadata.getHostName()
            hostMetadataJson.state = metadata.getHostMetadataState().name
            hostMetadataJson
        }).collect(Collectors.toSet<HostMetadataJson>())
    }

    private fun getRecipeIds(recipes: Set<Recipe>): Set<Long> {
        return recipes.stream().map(Function<Recipe, Long> { it.getId() }).collect(Collectors.toSet<Long>())
    }
}
