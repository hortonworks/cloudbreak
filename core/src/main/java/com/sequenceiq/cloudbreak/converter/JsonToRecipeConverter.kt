package com.sequenceiq.cloudbreak.converter

import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPE_TIMEOUT

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.RecipeRequest
import com.sequenceiq.cloudbreak.domain.Recipe

@Component
class JsonToRecipeConverter : AbstractConversionServiceAwareConverter<RecipeRequest, Recipe>() {
    override fun convert(json: RecipeRequest): Recipe {
        val recipe = Recipe()
        recipe.name = json.name
        recipe.description = json.description
        recipe.keyValues = json.properties
        recipe.plugins = json.plugins
        recipe.timeout = if (json.timeout == null) DEFAULT_RECIPE_TIMEOUT else json.timeout
        return recipe
    }
}
