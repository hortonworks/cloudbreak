package com.sequenceiq.cloudbreak.shell.commands.base

import com.sequenceiq.cloudbreak.shell.util.TopologyUtil.checkTopologyForResource

import org.apache.http.MethodNotSupportedException
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.TemplateRequest
import com.sequenceiq.cloudbreak.api.model.TemplateResponse
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands
import com.sequenceiq.cloudbreak.shell.commands.TemplateCommands
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class BaseTemplateCommands(private val shellContext: ShellContext) : BaseCommands, TemplateCommands {

    @CliAvailabilityIndicator(value = "template list")
    override fun listAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "template list", help = "Shows the currently available cloud templates")
    @Throws(Exception::class)
    override fun list(): String {
        try {
            val publics = shellContext.cloudbreakClient().templateEndpoint().publics
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliAvailabilityIndicator(value = "template show")
    override fun showAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @Throws(Exception::class)
    override fun show(id: Long?, name: String?): String {
        try {
            if (id != null) {
                return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(shellContext.cloudbreakClient().templateEndpoint()[id]), "FIELD", "VALUE")
            } else if (name != null) {
                val aPublic = shellContext.cloudbreakClient().templateEndpoint().getPublic(name)
                if (aPublic != null) {
                    return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(aPublic), "FIELD", "VALUE")
                }
            }
            return "No template specified."
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "template show --id", help = "Shows the template by its id")
    @Throws(Exception::class)
    override fun showById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return show(id, null)
    }

    @CliCommand(value = "template show --name", help = "Shows the template by its name")
    @Throws(Exception::class)
    override fun showByName(@CliOption(key = "", mandatory = true) name: String): String {
        return show(null, name)
    }

    override fun selectAvailable(): Boolean {
        return false
    }

    @Throws(Exception::class)
    override fun select(id: Long?, name: String?): String {
        throw MethodNotSupportedException("Select is not supported on templates")
    }

    @Throws(Exception::class)
    override fun selectById(id: Long?): String {
        return select(id, null)
    }

    @Throws(Exception::class)
    override fun selectByName(name: String): String {
        return select(null, name)
    }

    @CliAvailabilityIndicator(value = "template delete")
    override fun deleteAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @Throws(Exception::class)
    override fun delete(id: Long?, name: String?): String {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().templateEndpoint().delete(id)
                return String.format("Template has been deleted, id: %s", id)
            } else if (name != null) {
                shellContext.cloudbreakClient().templateEndpoint().deletePublic(name)
                return String.format("Template has been deleted, name: %s", name)
            }
            return "No template specified."
        } catch (ex: Exception) {
            return ex.toString()
        }

    }

    @CliCommand(value = "template delete --id", help = "Shows the template by its id")
    @Throws(Exception::class)
    override fun deleteById(@CliOption(key = "", mandatory = true) id: Long?): String {
        return delete(id, null)
    }

    @CliCommand(value = "template delete --name", help = "Shows the template by its name")
    @Throws(Exception::class)
    override fun deleteByName(@CliOption(key = "", mandatory = true) name: String): String {
        return delete(null, name)
    }

    override fun createTemplateAvailable(platform: String): Boolean {
        return !shellContext.isMarathonMode
    }

    override fun create(name: String, instanceType: String, volumeCount: Int?, volumeSize: Int?, volumeType: String, publicInAccount: Boolean?,
                        description: String, parameters: Map<String, Any>, platformId: Long?, platform: String): String {
        var publicInAccount = publicInAccount
        publicInAccount = if (publicInAccount == null) false else publicInAccount

        try {
            val id: IdJson
            val templateRequest = TemplateRequest()
            templateRequest.cloudPlatform = platform
            templateRequest.name = name
            templateRequest.description = description
            templateRequest.instanceType = instanceType
            templateRequest.volumeCount = volumeCount
            templateRequest.volumeSize = volumeSize
            templateRequest.volumeType = volumeType
            templateRequest.parameters = parameters
            if (platformId != null) {
                checkTopologyForResource(shellContext.cloudbreakClient().topologyEndpoint().publics, platformId, platform)
            }
            templateRequest.topologyId = platformId

            if (publicInAccount) {
                id = shellContext.cloudbreakClient().templateEndpoint().postPublic(templateRequest)
            } else {
                id = shellContext.cloudbreakClient().templateEndpoint().postPrivate(templateRequest)
            }
            createOrSelectBlueprintHint()
            return String.format(CREATE_SUCCESS_MESSAGE, id.id, name)
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    override fun shellContext(): ShellContext {
        return shellContext
    }

    @Throws(Exception::class)
    private fun createOrSelectBlueprintHint() {
        if (shellContext.isCredentialAccessible && shellContext.isBlueprintAccessible) {
            shellContext.setHint(Hints.CONFIGURE_INSTANCEGROUP)
        } else if (!shellContext.isBlueprintAccessible) {
            shellContext.setHint(Hints.SELECT_BLUEPRINT)
        } else if (!shellContext.isCredentialAccessible) {
            shellContext.setHint(Hints.SELECT_CREDENTIAL)
        } else if (shellContext.isCredentialAvailable && shellContext.activeHostGroups.size == shellContext.instanceGroups.size && shellContext.activeHostGroups.size != 0) {
            shellContext.setHint(Hints.CREATE_STACK)
        } else if (shellContext.isStackAccessible) {
            shellContext.setHint(Hints.CREATE_STACK)
        } else {
            shellContext.setHint(Hints.NONE)
        }
    }

    companion object {

        private val CREATE_SUCCESS_MESSAGE = "Template created with id: '%d' and name: '%s'"
    }

}
