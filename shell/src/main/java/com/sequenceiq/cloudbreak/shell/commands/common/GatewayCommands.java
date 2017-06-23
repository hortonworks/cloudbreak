package com.sequenceiq.cloudbreak.shell.commands.common;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;

import com.sequenceiq.cloudbreak.shell.model.ShellContext;

/**
 * Gateway commands used in the shell. Delegating the commands
 * to the Cloudbreak server via a Groovy based client.
 */
public class GatewayCommands implements CommandMarker {

    private ShellContext shellContext;

    public GatewayCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    /**
     * Checks whether the gateway show command is available or not.
     *
     * @return true if available false otherwise
     */
    @CliAvailabilityIndicator("gateway show")
    public boolean isGatewayShowCommandAvailable() {
        return true;
    }

    /**
     * Provides infomration about gateway settings.
     *
     * @return information about gateway settings
     */
    @CliCommand(value = "gateway show", help = "Shows information about gateway settings")
    public String gatewayInformation() {
        return getRow("multiple gateway mode", shellContext.isMultipleGatewayEnabled());
    }

    @CliAvailabilityIndicator("gateway enableMultipleGateway")
    public boolean isEnableMultiGatewayCommandAvailable() {
        return true;
    }

    @CliCommand(value = "gateway enableMultipleGateway", help = "Enable multiple gateway mode")
    public String enableMultiGateway() {
        shellContext.setMultipleGatewayEnabled(true);
        return "Multiple gateway mode is enabled.";
    }

    @CliAvailabilityIndicator("gateway disableMultipleGateway")
    public boolean isDisableMultiGatewayCommandAvailable() {
        return true;
    }

    @CliCommand(value = "gateway disableMultipleGateway", help = "Disable multiple gateway mode")
    public String disableMultiGateway() {
        shellContext.setMultipleGatewayEnabled(false);
        return "Multiple gateway mode is disabled.";
    }

    private String getRow(String name, Object value) {
        return String.format("%s: %s\n", name, value == null ? "" : value.toString());
    }
}
