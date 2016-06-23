package com.sequenceiq.cloudbreak.service.cluster.flow

import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine.DEFAULT_RECIPE_TIMEOUT

import java.util.ArrayList
import java.util.HashMap

import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.domain.Recipe

@Component
class ConsulRecipeBuilder : RecipeBuilder {

    override fun buildRecipes(recipeScripts: List<RecipeScript>, properties: Map<String, String>): List<Recipe> {
        val recipes = ArrayList<Recipe>()
        var index = 0
        for (script in recipeScripts) {
            val recipe = Recipe()
            val recipeName = RandomStringUtils.random(NAME_LENGTH, true, true)
            recipe.name = recipeName
            val tomlContent = StringBuilder().append(String.format("[plugin]\nname=\"%s\"\ndescription=\"\"\nversion=\"1.0\"\n", recipeName)).append("maintainer_name=\"Cloudbreak\"\n").append("[plugin.config]\n[plugin.compatibility]").toString()
            val pluginContentBuilder = StringBuilder()
            pluginContentBuilder.append("plugin.toml:").append(Base64.encodeBase64String(tomlContent.toByteArray())).append("\n")
            val plugins = HashMap<String, ExecutionType>()
            when (script.clusterLifecycleEvent) {
                ClusterLifecycleEvent.PRE_INSTALL -> pluginContentBuilder.append("recipe-pre-install:").append(Base64.encodeBase64String(script.script.toByteArray())).append("\n")
                ClusterLifecycleEvent.POST_INSTALL -> pluginContentBuilder.append("recipe-post-install:").append(Base64.encodeBase64String(script.script.toByteArray())).append("\n")
                else -> throw UnsupportedOperationException("Cluster lifecycle event " + script.clusterLifecycleEvent + " is not supported")
            }
            plugins.put("base64://" + Base64.encodeBase64String(pluginContentBuilder.toString().toByteArray()), script.executionType)
            recipe.plugins = plugins
            recipe.timeout = DEFAULT_RECIPE_TIMEOUT
            if (index == 0) {
                recipe.keyValues = properties
            } else {
                recipe.keyValues = HashMap<String, String>()
            }
            index++
            recipes.add(recipe)
        }

        return recipes
    }

    companion object {

        private val NAME_LENGTH = 10
    }
}
