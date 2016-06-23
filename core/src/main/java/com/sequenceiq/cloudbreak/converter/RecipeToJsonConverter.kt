package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.RecipeResponse
import com.sequenceiq.cloudbreak.domain.Recipe

@Component
class RecipeToJsonConverter : AbstractConversionServiceAwareConverter<Recipe, RecipeResponse>() {
    override fun convert(recipe: Recipe): RecipeResponse {
        val json = RecipeResponse()
        json.name = recipe.name
        json.description = recipe.description
        json.properties = recipe.keyValues
        json.plugins = recipe.plugins
        json.id = recipe.id
        json.timeout = recipe.timeout
        json.isPublicInAccount = recipe.isPublicInAccount
        return json
    }
}
