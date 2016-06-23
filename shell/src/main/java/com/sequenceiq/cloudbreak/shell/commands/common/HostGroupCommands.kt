package com.sequenceiq.cloudbreak.shell.commands.common

import java.util.HashSet

import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand
import org.springframework.shell.core.annotation.CliOption

import com.google.common.base.Function
import com.google.common.base.Splitter
import com.google.common.collect.FluentIterable
import com.sequenceiq.cloudbreak.api.model.RecipeResponse
import com.sequenceiq.cloudbreak.shell.completion.HostGroup
import com.sequenceiq.cloudbreak.shell.model.HostgroupEntry
import com.sequenceiq.cloudbreak.shell.model.ShellContext

class HostGroupCommands(private val shellContext: ShellContext) : CommandMarker {

    val isCreateHostGroupAvailable: Boolean
        @CliAvailabilityIndicator(value = "hostgroup configure")
        get() = (shellContext.isBlueprintAvailable && shellContext.isCredentialAvailable || shellContext.isStackAvailable) && !shellContext.isMarathonMode

    val isShowHostGroupAvailable: Boolean
        @CliAvailabilityIndicator(value = "hostgroup show")
        get() = !shellContext.isMarathonMode

    @CliCommand(value = "hostgroup configure", help = "Configure host groups")
    @Throws(Exception::class)
    fun createHostGroup(
            @CliOption(key = "hostgroup", mandatory = true, help = "Name of the hostgroup") hostgroup: HostGroup,
            @CliOption(key = "recipeIds", mandatory = false, help = "A comma separated list of recipe ids") recipeIds: String?,
            @CliOption(key = "recipeNames", mandatory = false, help = "A comma separated list of recipe names") recipeNames: String?): String {
        try {
            val recipeIdSet = HashSet<Long>()
            if (recipeIds != null) {
                recipeIdSet.addAll(getRecipeIds(recipeIds, RecipeParameterType.ID))
            }
            if (recipeNames != null) {
                recipeIdSet.addAll(getRecipeIds(recipeNames, RecipeParameterType.NAME))
            }
            shellContext.putHostGroup(hostgroup.name,
                    HostgroupEntry(shellContext.instanceGroups[hostgroup.name].nodeCount, recipeIdSet))
            return shellContext.outputTransformer().render(shellContext.hostGroups, "hostgroup")
        } catch (ex: Exception) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex)
        }

    }

    @CliCommand(value = "hostgroup show", help = "Configure host groups")
    @Throws(Exception::class)
    fun showHostGroup(): String {
        return shellContext.outputTransformer().render(shellContext.hostGroups, "hostgroup")
    }

    private enum class RecipeParameterType {
        ID, NAME
    }

    private fun getRecipeIds(inputs: String, type: RecipeParameterType): Set<Long> {
        return FluentIterable.from(Splitter.on(",").omitEmptyStrings().trimResults().split(inputs)).transform(Function<kotlin.String, kotlin.Long> { input ->
            try {
                var resp: RecipeResponse? = null
                when (type) {
                    HostGroupCommands.RecipeParameterType.ID -> resp = shellContext.cloudbreakClient().recipeEndpoint()[java.lang.Long.valueOf(input)]
                    HostGroupCommands.RecipeParameterType.NAME -> resp = shellContext.cloudbreakClient().recipeEndpoint().getPublic(input)
                    else -> throw UnsupportedOperationException()
                }
                return@Function resp.id
            } catch (e: Exception) {
                throw RuntimeException(e.message)
            }
        }).toSet()
    }
}
