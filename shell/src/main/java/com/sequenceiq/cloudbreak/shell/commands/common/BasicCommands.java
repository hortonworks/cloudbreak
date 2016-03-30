package com.sequenceiq.cloudbreak.shell.commands.common;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;

import com.sequenceiq.cloudbreak.shell.model.ShellContext;

/**
 * Basic commands used in the shell. Delegating the commands
 * to the Cloudbreak server via a Groovy based client.
 */
public class BasicCommands implements CommandMarker {

    private ShellContext shellContext;

    public BasicCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    /**
     * Checks whether the hint command is available or not.
     *
     * @return true if available false otherwise
     */
    @CliAvailabilityIndicator("hint")
    public boolean isHintCommandAvailable() {
        return true;
    }

    /**
     * Provides some hints what you can do in the current context.
     *
     * @return hint message
     */
    @CliCommand(value = "hint", help = "Shows some hints")
    public String hint() {
        return shellContext.getHint();
    }

    @CliAvailabilityIndicator("context")
    public boolean isContextCommandAvailable() {
        return true;
    }

    @CliCommand(value = "context", help = "Shows some context")
    public String context() {
        StringBuilder sb = new StringBuilder();
        sb.append(getRow("blueprintId", shellContext.getBlueprintId()));
        sb.append(getRow("credentialId", shellContext.getCredentialId()));
        sb.append(getRow("networkId", shellContext.getActiveNetworkId()));
        sb.append(getRow("securityGroupId", shellContext.getActiveSecurityGroupId()));
        sb.append(getRow("stackId", shellContext.getStackId()));
        sb.append(getRow("stackName", shellContext.getStackName()));
        sb.append(getRow("recipeId", shellContext.getRecipeId()));
        return sb.toString();
    }

    private String getRow(String name, Object value) {
        return String.format("%s: %s\n", name, value == null ? "" : value.toString());
    }
}
