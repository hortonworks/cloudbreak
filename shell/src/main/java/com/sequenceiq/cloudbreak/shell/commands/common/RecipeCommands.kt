package com.sequenceiq.cloudbreak.shell.commands.common

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URL
import java.util.HashMap

import org.apache.commons.codec.binary.Base64
import org.apache.commons.io.IOUtils
import org.apache.http.MethodNotSupportedException
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.ExecutionType
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.RecipeRequest
import com.sequenceiq.cloudbreak.api.model.RecipeResponse
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.completion.PluginExecutionType
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class RecipeCommands(private val shellContext: ShellContext) : BaseCommands {


    @CliAvailabilityIndicator(value = "recipe list")
    override fun listAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "recipe list", help = "Shows the currently available recipes")
    override fun list(): String {
        try {
            val publics = shellContext.cloudbreakClient().recipeEndpoint().publics
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    override fun selectAvailable(): Boolean {
        return false
    }

    @Throws(Exception::class)
    override fun select(id: Long?, name: String?): String {
        throw MethodNotSupportedException("Recipe select command not supported.")
    }

    @Throws(Exception::class)
    override fun selectById(id: Long?): String {
        return select(id, null)
    }

    @Throws(Exception::class)
    override fun selectByName(name: String): String {
        return select(null, name)
    }

    @CliAvailabilityIndicator(value = *arrayOf("recipe create --withParameters", "recipe create --withFile"))
    fun createAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "recipe create --withFile", help = "Add a new recipe with either --url or --file")
    fun create(
            @CliOption(key = "url", mandatory = false, help = "URL of the Recipe to download from") url: String,
            @CliOption(key = "file", mandatory = false, help = "File which contains the Recipe") file: File?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the recipe is public in the account") publicInAccount: Boolean?): String {
        var publicInAccount = publicInAccount
        try {
            val json = if (file == null) IOUtils.toString(URL(url)) else IOUtils.toString(FileInputStream(file))
            publicInAccount = if (publicInAccount == null) false else publicInAccount
            val id: IdJson
            val recipeRequest = shellContext.objectMapper().readValue<RecipeRequest>(json, RecipeRequest::class.java)
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().recipeEndpoint().postPublic(recipeRequest)
            } else {
                id = shellContext.cloudbreakClient().recipeEndpoint().postPrivate(recipeRequest)
            }
            return String.format(CREATE_SUCCESS_MESSAGE, id.id, recipeRequest.name)
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "recipe create --withParameters", help = "Create and store a new recipe")
    fun storeRecipe(
            @CliOption(key = "name", mandatory = true, help = "Unique name of the recepie") name: String,
            @CliOption(key = "description", help = "Description of the recepie") description: String?,
            @CliOption(key = "executionType", mandatory = true, help = "Type of recepie execution") executionType: PluginExecutionType,
            @CliOption(key = "preInstallScriptFile", help = "Path of the pre install script file") preInstallScriptFile: File?,
            @CliOption(key = "postInstallScriptFile", help = "Path of the post install script file") postInstallScriptFile: File?,
            @CliOption(key = "timeout", help = "Timeout of the script execution") timeout: Int?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the recipe is public in the account") publicInAccount: Boolean?): String {
        if (preInstallScriptFile != null && !preInstallScriptFile.exists()) {
            return "Pre install script file not exists."
        } else if (postInstallScriptFile != null && !postInstallScriptFile.exists()) {
            return "Post install script file not exists."
        } else if (preInstallScriptFile == null && postInstallScriptFile == null) {
            return "At least one script is required."
        }
        try {
            val tomlContent = String.format("[plugin]\nname=\"%s\"\ndescription=\"%s\"\nversion=\"1.0\"\n", name, description ?: "")
            val pluginContentBuilder = StringBuilder().append("plugin.toml:").append(Base64.encodeBase64String(tomlContent.toByteArray())).append("\n")
            if (preInstallScriptFile != null) {
                addScriptContent(pluginContentBuilder, "recipe-pre-install", preInstallScriptFile)
            }
            if (postInstallScriptFile != null) {
                addScriptContent(pluginContentBuilder, "recipe-post-install", postInstallScriptFile)
            }

            val plugins = HashMap<String, ExecutionType>()
            plugins.put("base64://" + Base64.encodeBase64String(pluginContentBuilder.toString().toByteArray()),
                    ExecutionType.valueOf(executionType.name))

            val recipeRequest = RecipeRequest()
            recipeRequest.name = name
            recipeRequest.description = description
            recipeRequest.timeout = timeout
            recipeRequest.plugins = plugins
            val id: IdJson
            if (publicInAccount!!) {
                id = shellContext.cloudbreakClient().recipeEndpoint().postPublic(recipeRequest)
            } else {
                id = shellContext.cloudbreakClient().recipeEndpoint().postPrivate(recipeRequest)
            }
            return String.format(CREATE_SUCCESS_MESSAGE, id.id, recipeRequest.name)
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("recipe show --id", "recipe show --name"))
    override fun showAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "recipe show", help = "Shows the properties of the specified recipe")
    override fun show(id: Long?, name: String?): String {
        try {
            var recipeMap: RecipeResponse? = null
            if (id != null) {
                recipeMap = shellContext.cloudbreakClient().recipeEndpoint()[id]
            } else if (name != null) {
                recipeMap = shellContext.cloudbreakClient().recipeEndpoint().getPublic(name)
            } else {
                return "Recipe not specified."
            }
            val map = HashMap<String, String>()
            map.put("id", recipeMap.id!!.toString())
            map.put("name", recipeMap.name)
            map.put("description", recipeMap.description)
            map.put("timeout", recipeMap.timeout!!.toString())

            return shellContext.outputTransformer().render<Map<String, String>>(map, "FIELD", "INFO") + "\n\n"
            +shellContext.outputTransformer().render<Map<String, String>>(recipeMap.properties, "CONSUL-KEY", "VALUE") + "\n\n"
            +shellContext.outputTransformer().render<Map<String, ExecutionType>>(recipeMap.plugins, "PLUGIN", "EXECUTION_TYPE") + "\n\n"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "recipe show --id", help = "Show the recipe by its id")
    @Throws(Exception::class)
    override fun showById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return show(id, null)
    }

    @CliCommand(value = "recipe show --name", help = "Show the recipe by its name")
    @Throws(Exception::class)
    override fun showByName(@CliOption(key = "", mandatory = true) name: String): String {
        return show(null, name)
    }

    @CliAvailabilityIndicator(value = *arrayOf("recipe delete --id", "recipe delete --name"))
    override fun deleteAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "recipe delete", help = "Delete the recipe by its id or name")
    override fun delete(id: Long?, name: String?): String {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().recipeEndpoint().delete(id)
                return String.format("Recipe deleted with %s id", id)
            } else if (name != null) {
                shellContext.cloudbreakClient().recipeEndpoint().deletePublic(name)
                return String.format("Recipe deleted with %s name", name)
            }
            return "Recipe not specified (select recipe by --id or --name)"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "recipe delete --id", help = "Delete the recipe by its id")
    @Throws(Exception::class)
    override fun deleteById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return delete(id, null)
    }

    @CliCommand(value = "recipe delete --name", help = "Delete the recipe by its name")
    @Throws(Exception::class)
    override fun deleteByName(@CliOption(key = "", mandatory = true) name: String): String {
        return delete(null, name)
    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

    @Throws(IOException::class)
    private fun addScriptContent(builder: StringBuilder, name: String, scriptFile: File) {
        val script = IOUtils.toString(FileInputStream(scriptFile))
        builder.append(name).append(":").append(Base64.encodeBase64String(script.toByteArray())).append("\n")
    }

    companion object {

        private val CREATE_SUCCESS_MESSAGE = "Recipe created with id: %s and name: '%s'"
    }

}
