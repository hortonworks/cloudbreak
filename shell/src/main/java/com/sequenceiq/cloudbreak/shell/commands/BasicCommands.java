package com.sequenceiq.cloudbreak.shell.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;

/**
 * Basic commands used in the shell. Delegating the commands
 * to the Cloudbreak server via a Groovy based client.
 */
@Component
public class BasicCommands implements CommandMarker {

    @Autowired
    private CloudbreakContext context;

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
        return context.getHint();
    }

    @CliAvailabilityIndicator("context")
    public boolean isContextCommandAvailable() {
        return true;
    }

    @CliCommand(value = "context", help = "Shows some context")
    public String context() {
        StringBuilder sb = new StringBuilder();
        sb.append(getRow("blueprintId", context.getBlueprintId()));
        sb.append(getRow("credentialId", context.getCredentialId()));
        sb.append(getRow("networkId", context.getActiveNetworkId()));
        sb.append(getRow("securityGroupId", context.getActiveSecurityGroupId()));
        sb.append(getRow("stackId", context.getStackId()));
        sb.append(getRow("stackName", context.getStackName()));
        sb.append(getRow("recipeId", context.getRecipeId()));
        return sb.toString();
    }

    private String getRow(String name, String value) {
        return String.format("%s: %s\n", name, value);
    }
}
