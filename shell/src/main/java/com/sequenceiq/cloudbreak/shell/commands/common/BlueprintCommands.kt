package com.sequenceiq.cloudbreak.shell.commands.common

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URL
import java.util.ArrayList
import java.util.HashMap

import org.apache.commons.io.IOUtils
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.fasterxml.jackson.databind.JsonNode
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class BlueprintCommands(private val shellContext: ShellContext) : BaseCommands {

    @CliAvailabilityIndicator(value = "blueprint create")
    fun createAvailable(): Boolean {
        return true
    }

    @CliCommand(value = "blueprint create", help = "Add a new blueprint with either --url or --file")
    fun create(
            @CliOption(key = "description", mandatory = true, help = "Description of the blueprint to download from") description: String,
            @CliOption(key = "name", mandatory = true, help = "Name of the blueprint to download from") name: String,
            @CliOption(key = "url", mandatory = false, help = "URL of the blueprint to download from") url: String,
            @CliOption(key = "file", mandatory = false, help = "File which contains the blueprint") file: File?,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the blueprint is public in the account") publicInAccount: Boolean?): String {
        var publicInAccount = publicInAccount
        try {
            val message: String
            publicInAccount = if (publicInAccount == null) false else publicInAccount
            val json = if (file == null) IOUtils.toString(URL(url)) else IOUtils.toString(FileInputStream(file))
            if (json != null) {
                val blueprintRequest = BlueprintRequest()
                blueprintRequest.name = name
                blueprintRequest.description = description
                blueprintRequest.setAmbariBlueprint(shellContext.objectMapper().readValue<JsonNode>(json, JsonNode::class.java))
                val id: String
                if (publicInAccount) {
                    id = shellContext.cloudbreakClient().blueprintEndpoint().postPublic(blueprintRequest).id!!.toString()
                } else {
                    id = shellContext.cloudbreakClient().blueprintEndpoint().postPrivate(blueprintRequest).id!!.toString()
                }
                shellContext.addBlueprint(id)
                if (shellContext.cloudbreakClient().blueprintEndpoint().publics.isEmpty()) {
                    shellContext.setHint(
                            if (shellContext.isMarathonMode) Hints.CONFIGURE_MARATHON_HOSTGROUP else Hints.CONFIGURE_INSTANCEGROUP)
                } else {
                    shellContext.setHint(if (shellContext.isMarathonMode) Hints.CONFIGURE_MARATHON_HOSTGROUP else Hints.SELECT_STACK)
                }
                message = String.format("Blueprint created with id: '%s' and name: '%s'", id, name)
            } else {
                message = "No blueprint specified"
            }
            return message
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = "blueprint list")
    override fun listAvailable(): Boolean {
        return true
    }

    @CliCommand(value = "blueprint list", help = "Shows the currently available blueprints")
    @Throws(Exception::class)
    override fun list(): String {
        try {
            val publics = shellContext.cloudbreakClient().blueprintEndpoint().publics
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = *arrayOf("blueprint select --id", "blueprint select --name"))
    override fun selectAvailable(): Boolean {
        return shellContext.isBlueprintAccessible
    }

    override fun select(id: Long?, name: String?): String {
        try {
            if (id != null) {
                if (shellContext.cloudbreakClient().blueprintEndpoint()[id] != null) {
                    shellContext.addBlueprint(id.toString())
                    shellContext.resetMarathonHostGroups()
                    shellContext.setHint(
                            if (shellContext.isMarathonMode) Hints.CONFIGURE_MARATHON_HOSTGROUP else Hints.CONFIGURE_INSTANCEGROUP)
                    return String.format("Blueprint has been selected, id: %s", id)
                }
            } else if (name != null) {
                val blueprint = shellContext.cloudbreakClient().blueprintEndpoint().getPublic(name)
                if (blueprint != null) {
                    shellContext.addBlueprint(blueprint.id!!.toString())
                    shellContext.resetMarathonHostGroups()
                    shellContext.setHint(
                            if (shellContext.isMarathonMode) Hints.CONFIGURE_MARATHON_HOSTGROUP else Hints.CONFIGURE_INSTANCEGROUP)
                    return String.format("Blueprint has been selected, name: %s", name)
                }
            }
            return "No blueprint specified (select a blueprint by --id or --name)"
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "blueprint select --id", help = "Delete the blueprint by its id")
    @Throws(Exception::class)
    override fun selectById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return select(id, null)
    }

    @CliCommand(value = "blueprint select --name", help = "Delete the blueprint by its name")
    @Throws(Exception::class)
    override fun selectByName(@CliOption(key = "", mandatory = true) name: String): String {
        return select(null, name)
    }

    @CliAvailabilityIndicator(value = *arrayOf("blueprint show --id", "blueprint show --name"))
    override fun showAvailable(): Boolean {
        return true
    }

    override fun show(id: Long?, name: String?): String {
        try {
            val blueprintResponse: BlueprintResponse
            if (id != null) {
                blueprintResponse = shellContext.cloudbreakClient().blueprintEndpoint()[id]
            } else if (name != null) {
                blueprintResponse = shellContext.cloudbreakClient().blueprintEndpoint().getPublic(name)
            } else {
                return "No blueprints specified."
            }
            return shellContext.outputTransformer().render(
                    shellContext.responseTransformer().transformObjectToStringMap(blueprintResponse, "ambariBlueprint"), "FIELD", "INFO")
            +"\n\n"
            +shellContext.outputTransformer().render(getComponentMap(blueprintResponse.ambariBlueprint), "HOSTGROUP", "COMPONENT")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "blueprint show --id", help = "Show the blueprint by its id")
    @Throws(Exception::class)
    override fun showById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return show(id, null)
    }

    @CliCommand(value = "blueprint show --name", help = "Show the blueprint by its name")
    @Throws(Exception::class)
    override fun showByName(@CliOption(key = "", mandatory = true) name: String): String {
        return show(null, name)
    }

    @CliAvailabilityIndicator(value = *arrayOf("blueprint delete --id", "blueprint delete --name"))
    override fun deleteAvailable(): Boolean {
        return true
    }

    @Throws(Exception::class)
    override fun delete(id: Long?, name: String?): String {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().blueprintEndpoint().delete(id)
                return String.format("Blueprint deleted with %s id", id)
            } else if (name != null) {
                shellContext.cloudbreakClient().blueprintEndpoint().deletePublic(name)
                return String.format("Blueprint deleted with %s name", name)
            } else {
                return "No blueprint specified (select a blueprint by --id or --name)"
            }
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "blueprint delete --id", help = "Delete the blueprint by its id")
    @Throws(Exception::class)
    override fun deleteById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return delete(id, null)
    }

    @CliCommand(value = "blueprint delete --name", help = "Delete the blueprint by its name")
    @Throws(Exception::class)
    override fun deleteByName(@CliOption(key = "", mandatory = true) name: String): String {
        return delete(null, name)
    }

    @CliAvailabilityIndicator(value = "blueprint defaults")
    fun defaultAvailable(): Boolean {
        return true
    }

    @CliCommand(value = "blueprint defaults", help = "Adds the default blueprints to Ambari")
    fun defaults(): String {
        val message = "Default blueprints added"
        try {
            shellContext.cloudbreakClient().blueprintEndpoint().publics
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("Failed to add the default blueprints: " + ex.message)
        }

        return message
    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

    private fun getComponentMap(json: String): Map<String, List<String>> {
        var map: MutableMap<String, List<String>> = HashMap()
        try {
            val hostGroups = shellContext.objectMapper().readTree(json.toByteArray()).get("host_groups")
            for (hostGroup in hostGroups) {
                val components = ArrayList<String>()
                val componentsNodes = hostGroup.get("components")
                for (componentsNode in componentsNodes) {
                    components.add(componentsNode.get("name").asText())
                }
                map.put(hostGroup.get("name").asText(), components)
            }
        } catch (e: IOException) {
            map = HashMap<String, List<String>>()
        }

        return map
    }

}
