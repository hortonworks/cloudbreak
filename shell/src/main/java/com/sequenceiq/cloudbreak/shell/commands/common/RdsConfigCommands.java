package com.sequenceiq.cloudbreak.shell.commands.common;


import org.springframework.shell.core.annotation.CliAvailabilityIndicator;

import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class RdsConfigCommands {

    private ShellContext shellContext;

    public RdsConfigCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = "rdsconfig create")
    public boolean createAvailable() {
        return true;
    }

}
