package com.sequenceiq.cloudbreak.shell.commands.marathon

import java.util.HashSet

import javax.inject.Inject

import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse
import com.sequenceiq.cloudbreak.api.model.IdJson
import com.sequenceiq.cloudbreak.api.model.OrchestratorRequest
import com.sequenceiq.cloudbreak.api.model.StackRequest
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.cloudbreak.shell.completion.ConstraintName
import com.sequenceiq.cloudbreak.shell.completion.HostGroup
import com.sequenceiq.cloudbreak.shell.model.FocusType
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.MarathonHostgroupEntry
import com.sequenceiq.cloudbreak.shell.model.ShellContext
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer

@Component
class MarathonCommands : CommandMarker {
    @Inject
    private val shellContext: ShellContext? = null
    @Inject
    private val cloudbreakClient: CloudbreakClient? = null
    @Inject
    private val exceptionTransformer: ExceptionTransformer? = null
    @Inject
    private val responseTransformer: ResponseTransformer<Collection<Any>>? = null

    val isHintCommandAvailable: Boolean
        @CliAvailabilityIndicator("mode")
        get() = true

    val isMarathonHintCommandAvailable: Boolean
        @CliAvailabilityIndicator("marathon")
        get() = shellContext!!.isMarathonMode

    @CliCommand(value = "mode --MARATHON", help = "Change to marathon mode")
    fun marathonMode() {
        if (shellContext!!.selectedMarathonStackName == null) {
            shellContext.setFocus(null, FocusType.MARATHON)
        } else {
            shellContext.setFocus(shellContext.selectedMarathonStackName, FocusType.MARATHON)
        }
        shellContext.setConstraints(cloudbreakClient!!.constraintTemplateEndpoint().publics)
        shellContext.setHint(Hints.MARATHON_STACK)
    }

    @CliCommand(value = "mode --DEFAULT", help = "Change to Marathon mode")
    fun rootMode() {
        shellContext!!.resetFocus()
        shellContext.setHint(Hints.CREATE_CREDENTIAL)
    }

    @CliCommand(value = "marathon import", help = "Import a marathon stack")
    fun createMarathonStack(
            @CliOption(key = "name", mandatory = true, help = "Name of the marathon stack") name: String,
            @CliOption(key = "marathonEndpoint", mandatory = true, help = "Endpoint of the marathon") marathonEndpoint: String): String {
        try {
            val stackRequest = StackRequest()
            val orchestratorRequest = OrchestratorRequest()
            orchestratorRequest.apiEndpoint = marathonEndpoint
            orchestratorRequest.type = "MARATHON"
            stackRequest.name = name
            stackRequest.orchestrator = orchestratorRequest
            return String.format("Marathon stack imported with id: '%d' and name: '%s'",
                    cloudbreakClient!!.stackEndpoint().postPublic(stackRequest).id, name)
        } catch (ex: Exception) {
            throw exceptionTransformer!!.transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "marathon show", help = "Show a marathon stack")
    fun showMarathonStack(
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon stack") name: String?,
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon stack") id: Long?): String {
        try {
            var response: StackResponse? = null
            if (id != null) {
                response = cloudbreakClient!!.stackEndpoint()[java.lang.Long.valueOf(id)]
            } else if (name != null) {
                response = cloudbreakClient!!.stackEndpoint().getPublic(name)
            }
            if (response == null || BYOS != response.platformVariant) {
                return "No marathon stack specified (select a marathon stack by --id or --name)"
            } else {
                return shellContext!!.outputTransformer().render(responseTransformer!!.transformObjectToStringMap(response.orchestrator), "FIELD", "VALUE")
            }
        } catch (ex: Exception) {
            throw exceptionTransformer!!.transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "marathon select", help = "Select a marathon stack")
    fun selectMarathonStack(
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon stack") name: String?,
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon stack") id: Long?): String {
        try {
            var response: StackResponse? = null
            if (id != null) {
                response = cloudbreakClient!!.stackEndpoint()[java.lang.Long.valueOf(id)]
            } else if (name != null) {
                response = cloudbreakClient!!.stackEndpoint().getPublic(name)
            }
            if (response == null) {
                if (BYOS != response!!.platformVariant) {
                    return "Not a marathon stack was specified."
                }
                return "Marathon stack not exist."
            } else {
                shellContext!!.selectedMarathonStackId = response.id
                shellContext.selectedMarathonStackName = response.name
                shellContext.resetMarathonHostGroups()
                shellContext.setFocus(response.name, FocusType.MARATHON)
                shellContext.setHint(Hints.SELECT_BLUEPRINT)
                return "Marathon stack selected with id: " + response.id!!
            }
        } catch (ex: Exception) {
            throw exceptionTransformer!!.transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "marathon list", help = "List of marathon stacks")
    fun listMarathonStack(): String {
        try {
            val responses = HashSet<StackResponse>()
            for (aPublic in cloudbreakClient!!.stackEndpoint().publics) {
                if (BYOS == aPublic.platformVariant) {
                    responses.add(aPublic)
                }
            }
            return shellContext!!.outputTransformer().render(responseTransformer!!.transformToMap(responses, "id", "name"), "ID", "INFO")
        } catch (ex: Exception) {
            throw exceptionTransformer!!.transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "marathon terminate", help = "Terminate a marathon stack")
    fun deleteMarathonStack(
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon stack") name: String?,
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon stack") id: Long?): String {
        try {
            if (id != null) {
                cloudbreakClient!!.stackEndpoint().delete(java.lang.Long.valueOf(id), true)
                if (id === shellContext!!.selectedMarathonStackId) {
                    shellContext!!.resetSelectedMarathonStackId()
                    shellContext.setHint(Hints.MARATHON_CLUSTER)
                }
                return String.format("Marathon stack has been deleted, id: %s", id)
            } else if (name != null) {
                val aPublic = cloudbreakClient!!.stackEndpoint().getPublic(name)
                cloudbreakClient.stackEndpoint().deletePublic(name, true)
                if (aPublic.id === shellContext!!.selectedMarathonStackId) {
                    shellContext!!.resetSelectedMarathonStackId()
                }
                return String.format("Marathon has been deleted, name: %s", name)
            }
            return "No marathon stack was specified."
        } catch (ex: Exception) {
            throw exceptionTransformer!!.transformToRuntimeException(ex)
        }

    }


    @CliCommand(value = "marathon constraint create", help = "Create a new marathon constraint")
    fun createMarathonTemplate(
            @CliOption(key = "name", mandatory = true, help = "Name of the marathon constraint") name: String,
            @CliOption(key = "cores", mandatory = true, help = "Cpu cores of the marathon constraint (0.1 - 64 core)") cpuCores: Double?,
            @CliOption(key = "memory", mandatory = true, help = "Memory in Mb of the marathon constraint (16mb - 128Gb)") memory: Double?,
            @CliOption(key = "diskSize", mandatory = true, help = "Disk in Gb of the marathon constraint (10Gb - 1000Gb)") disk: Double?,
            @CliOption(key = "description", mandatory = false, help = "Description of the marathon stack") description: String,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the constraint is public in the account") publicInAccount: Boolean?): String {
        var publicInAccount = publicInAccount
        var idJson: IdJson? = null
        try {
            val constraintTemplateRequest = ConstraintTemplateRequest()
            constraintTemplateRequest.name = name
            constraintTemplateRequest.cpu = cpuCores
            constraintTemplateRequest.description = description
            constraintTemplateRequest.disk = disk
            constraintTemplateRequest.memory = memory
            publicInAccount = if (publicInAccount == null) false else publicInAccount
            if (publicInAccount) {
                idJson = cloudbreakClient!!.constraintTemplateEndpoint().postPublic(constraintTemplateRequest)
            } else {
                idJson = cloudbreakClient!!.constraintTemplateEndpoint().postPrivate(constraintTemplateRequest)
            }
        } catch (ex: Exception) {
            exceptionTransformer!!.transformToRuntimeException(ex)
        }

        return "Marathon template was created with id: " + idJson!!.id!!
    }

    @CliCommand(value = "marathon constraint list", help = "Shows the currently available marathon constraints")
    fun listMarathonTemplates(): String {
        try {
            val publics = cloudbreakClient!!.constraintTemplateEndpoint().publics
            shellContext!!.setConstraints(publics)
            return shellContext.outputTransformer().render(responseTransformer!!.transformToMap(publics, "id", "name"), "ID", "INFO")
        } catch (ex: Exception) {
            throw exceptionTransformer!!.transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "marathon constraint delete", help = "Delete the marathon constraint by its id or name")
    fun deleteMarathonTemplate(
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon template") id: String?,
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon template") name: String?): Any {
        try {
            if (id != null) {
                cloudbreakClient!!.constraintTemplateEndpoint().delete(java.lang.Long.valueOf(id))
                shellContext!!.setConstraints(cloudbreakClient.constraintTemplateEndpoint().publics)
                return String.format("Marathon constraint has been deleted, id: %s", id)
            } else if (name != null) {
                cloudbreakClient!!.constraintTemplateEndpoint().deletePublic(name)
                shellContext!!.setConstraints(cloudbreakClient.constraintTemplateEndpoint().publics)
                return String.format("Marathon constraint has been deleted, name: %s", name)
            }
            return "No constraint specified."
        } catch (ex: Exception) {
            return ex.toString()
        }

    }

    @CliCommand(value = "marathon constraint show", help = "Shows the marathon constraint by its id or name")
    fun showTemplate(
            @CliOption(key = "id", mandatory = false, help = "Id of the marathon constraint") id: Long?,
            @CliOption(key = "name", mandatory = false, help = "Name of the marathon constraint") name: String): Any {
        try {
            val aPublic = getConstraintTemplateResponse(id, name)
            if (aPublic != null) {
                return shellContext!!.outputTransformer().render(responseTransformer!!.transformObjectToStringMap(aPublic), "FIELD", "VALUE")
            }
            return "No constraint was found."
        } catch (ex: Exception) {
            throw exceptionTransformer!!.transformToRuntimeException(ex)
        }

    }

    private fun getConstraintTemplateResponse(id: Long?, name: String): ConstraintTemplateResponse? {
        if (id != null) {
            return cloudbreakClient!!.constraintTemplateEndpoint()[java.lang.Long.valueOf(id)]
        } else {
            return cloudbreakClient!!.constraintTemplateEndpoint().getPublic(name)
        }
    }

    @CliCommand(value = "marathon hostgroup configure", help = "Configure hostgroups")
    @Throws(Exception::class)
    fun createHostGroup(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") hostgroup: HostGroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Count of the nodes in the hostgroup") nodecount: Int?,
            @CliOption(key = "constraintName", mandatory = true, help = "Name of the constraint") constraintTemplateName: ConstraintName): String {
        try {
            val constraintTemplateResponse = getConstraintTemplateResponse(null, constraintTemplateName.name)
            if (constraintTemplateResponse != null) {
                shellContext!!.putMarathonHostGroup(hostgroup.name, MarathonHostgroupEntry(nodecount, constraintTemplateName.name))
                if (shellContext.hostGroups.size == shellContext.activeHostGroups.size) {
                    shellContext.setHint(Hints.MARATHON_CLUSTER)
                }
                return shellContext.outputTransformer().render(shellContext.hostGroups, "hostgroup")
            } else {
                return "Constraint was not found."
            }
        } catch (ex: Exception) {
            throw exceptionTransformer!!.transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "marathon hostgroup show", help = "Show hostgroups")
    @Throws(Exception::class)
    fun listHostGroup(): String {
        try {
            return shellContext!!.outputTransformer().render(shellContext.hostGroups, "hostgroup")
        } catch (ex: Exception) {
            throw exceptionTransformer!!.transformToRuntimeException(ex)
        }

    }

    companion object {

        private val BYOS = "BYOS"
    }


}
