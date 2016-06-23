package com.sequenceiq.it.cloudbreak

import java.util.HashMap
import java.util.HashSet

import org.apache.commons.codec.binary.Base64
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.endpoint.RecipeEndpoint
import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.api.model.RecipeRequest
import com.sequenceiq.it.IntegrationTestContext

class RecipeCreationTest : AbstractCloudbreakIntegrationTest() {

    @Test
    @Parameters("name", "description", "preScript", "postScript")
    @Throws(Exception::class)
    fun testRecipeCreation(name: String, @Optional description: String, @Optional("") preScript: String, @Optional("") postScript: String) {
        // GIVEN
        val tomlContent = String.format("[plugin]\nname=\"%s\"\ndescription=\"%s\"\nversion=\"1.0\"\n", name, "")
        val pluginContentBuilder = StringBuilder().append("plugin.toml:").append(Base64.encodeBase64String(tomlContent.toByteArray())).append("\n")
        if (!preScript.isEmpty()) {
            addScriptContent(pluginContentBuilder, "recipe-pre-install", preScript)
        }
        if (!postScript.isEmpty()) {
            addScriptContent(pluginContentBuilder, "recipe-post-install", postScript)
        }

        val recipe = HashMap<String, Any>()
        recipe.put("name", name)
        recipe.put("description", description)
        val plugins = HashMap<String, String>()
        plugins.put("base64://" + Base64.encodeBase64String(pluginContentBuilder.toString().toByteArray()), "ALL_NODES")
        recipe.put("plugins", plugins)

        // WHEN
        val recipeRequest = RecipeRequest()
        recipeRequest.name = name
        recipeRequest.description = description
        val map = HashMap<String, ExecutionType>()
        map.put("base64://" + Base64.encodeBase64String(pluginContentBuilder.toString().toByteArray()), ExecutionType.ALL_NODES)
        recipeRequest.plugins = map
        val recipeEndpoint = cloudbreakClient.recipeEndpoint()
        val id = recipeEndpoint.postPrivate(recipeRequest).id!!.toString()

        addRecipeToContext(java.lang.Long.valueOf(id))
    }

    private fun addRecipeToContext(id: Long?) {
        val itContext = itContext
        var recipeIds: MutableSet<Long>? = itContext.getContextParam<Set<Any>>(CloudbreakITContextConstants.RECIPE_ID, Set<Any>::class.java)
        recipeIds = if (recipeIds == null) HashSet<Long>() else recipeIds
        recipeIds.add(id)
        itContext.putContextParam(CloudbreakITContextConstants.RECIPE_ID, recipeIds)
    }

    private fun addScriptContent(builder: StringBuilder, name: String, script: String) {
        builder.append(name).append(":").append(Base64.encodeBase64String((script + "\n").toByteArray())).append("\n")
    }
}
