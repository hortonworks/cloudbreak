package com.sequenceiq.cloudbreak.shell.commands.common

import java.util.HashMap
import java.util.HashSet

import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.google.common.primitives.Longs
import com.sequenceiq.cloudbreak.api.model.TemplateResponse
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroup
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateId
import com.sequenceiq.cloudbreak.shell.completion.InstanceGroupTemplateName
import com.sequenceiq.cloudbreak.shell.model.Hints
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry
import com.sequenceiq.cloudbreak.shell.model.InstanceGroupEntry
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class InstanceGroupCommands(private val shellContext: ShellContext) : CommandMarker {

    @CliAvailabilityIndicator(value = "instancegroup configure")
    fun createAvailable(): Boolean {
        return shellContext.isBlueprintAvailable && shellContext.isCredentialAvailable && !shellContext.isMarathonMode
    }


    @CliAvailabilityIndicator(value = "instancegroup show")
    fun showAvailable(): Boolean {
        return !shellContext.isMarathonMode
    }

    @CliCommand(value = "instancegroup configure", help = "Configure instance groups")
    @Throws(Exception::class)
    fun create(
            @CliOption(key = "instanceGroup", mandatory = true, help = "Name of the instanceGroup") instanceGroup: InstanceGroup,
            @CliOption(key = "nodecount", mandatory = true, help = "Nodecount for instanceGroup") nodeCount: Int?,
            @CliOption(key = "ambariServer", mandatory = true, help = "Ambari server will be installed here if true") ambariServer: Boolean,
            @CliOption(key = "templateId", mandatory = false, help = "TemplateId of the instanceGroup") instanceGroupTemplateId: InstanceGroupTemplateId?,
            @CliOption(key = "templateName", mandatory = false, help = "TemplateName of the instanceGroup") instanceGroupTemplateName: InstanceGroupTemplateName?): String {
        try {
            var templateId: String? = null
            if (instanceGroupTemplateId != null) {
                templateId = instanceGroupTemplateId.name
            } else if (instanceGroupTemplateName != null) {
                val aPublic = shellContext.cloudbreakClient().templateEndpoint().getPublic(instanceGroupTemplateName.name)
                if (aPublic != null) {
                    templateId = aPublic.id!!.toString()
                } else {
                    return String.format("Template not found by name: %s", instanceGroupTemplateName.name)
                }
            } else {
                return "Template name or id is not defined for instanceGroup (use --templateName or --templateId)"
            }
            val parsedTemplateId = Longs.tryParse(templateId)
            if (parsedTemplateId != null) {
                val map = HashMap<Long, Int>()
                map.put(parsedTemplateId, nodeCount)
                if (ambariServer) {
                    val ambariSpecified = shellContext.instanceGroups.values.stream().filter({ e -> e.type == "GATEWAY" }).findAny().isPresent()
                    if (ambariSpecified) {
                        return "Ambari server is already specified"
                    }
                    if (nodeCount !== 1) {
                        return "Allowed node count for Ambari server: 1"
                    }
                    shellContext.putInstanceGroup(instanceGroup.name, InstanceGroupEntry(parsedTemplateId, nodeCount, "GATEWAY"))
                } else {
                    shellContext.putInstanceGroup(instanceGroup.name, InstanceGroupEntry(parsedTemplateId, nodeCount, "CORE"))
                }
                shellContext.putHostGroup(instanceGroup.name, HostgroupEntry(nodeCount, HashSet<Long>()))
                if (shellContext.activeHostGroups.size == shellContext.instanceGroups.size && shellContext.activeHostGroups.size != 0) {
                    shellContext.setHint(Hints.SELECT_NETWORK)
                } else {
                    shellContext.setHint(Hints.CONFIGURE_HOSTGROUP)
                }
                return shellContext.outputTransformer().render(shellContext.instanceGroups, "instanceGroup")
            } else {
                return "TemplateId is not a number."
            }
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "instancegroup show", help = "Configure instance groups")
    @Throws(Exception::class)
    fun show(): String {
        if (shellContext.instanceGroups.isEmpty()) {
            return "List of instance groups is empty currently."
        } else {
            return shellContext.outputTransformer().render(shellContext.instanceGroups, "instanceGroup")
        }
    }
}
