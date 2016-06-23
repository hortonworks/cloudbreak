package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.domain.Recipe

interface RecipeBuilder {

    fun buildRecipes(recipeScripts: List<RecipeScript>, properties: Map<String, String>): List<Recipe>

}
