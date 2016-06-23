package com.sequenceiq.cloudbreak.shell.commands.common

import org.springframework.shell.core.CommandMarker
import org.springframework.shell.core.annotation.CliAvailabilityIndicator
import org.springframework.shell.core.annotation.CliCommand

import com.sequenceiq.cloudbreak.shell.model.ShellContext

/**
 * Basic commands used in the shell. Delegating the commands
 * to the Cloudbreak server via a Groovy based client.
 */
class BasicCommands(private val shellContext: ShellContext) : CommandMarker {

    /**
     * Checks whether the hint command is available or not.

     * @return true if available false otherwise
     */
    val isHintCommandAvailable: Boolean
        @CliAvailabilityIndicator("hint")
        get() = true

    /**
     * Provides some hints what you can do in the current context.

     * @return hint message
     */
    @CliCommand(value = "hint", help = "Shows some hints")
    fun hint(): String {
        return shellContext.hint
    }

    val isContextCommandAvailable: Boolean
        @CliAvailabilityIndicator("context")
        get() = true

    @CliCommand(value = "context", help = "Shows some context")
    fun context(): String {
        val sb = StringBuilder()
        sb.append(getRow("blueprintId", shellContext.blueprintId))
        sb.append(getRow("credentialId", shellContext.credentialId))
        sb.append(getRow("networkId", shellContext.activeNetworkId))
        sb.append(getRow("securityGroupId", shellContext.activeSecurityGroupId))
        sb.append(getRow("stackId", shellContext.stackId))
        sb.append(getRow("stackName", shellContext.stackName))
        sb.append(getRow("recipeId", shellContext.recipeId))
        return sb.toString()
    }

    private fun getRow(name: String, value: Any?): String {
        return String.format("%s: %s\n", name, if (value == null) "" else value.toString())
    }
}
