package com.sequenceiq.cloudbreak.shell.commands.common;

import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.shell.completion.DatabaseVendor;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class DatabaseCommands implements CommandMarker {

    private ShellContext shellContext;

    public DatabaseCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator("database configure")
    public boolean isConfigureAvailable() {
        return true;
    }

    @CliCommand(value = "database configure", help = "Configure Ambari remote database")
    public String configureDatabase(
            @CliOption(key = "vendor", mandatory = true, help = "Vendor of database") DatabaseVendor vendor,
            @CliOption(key = "host", mandatory = true, help = "Hostname of database") String host,
            @CliOption(key = "port", mandatory = true, help = "Port number of database") Integer port,
            @CliOption(key = "name", mandatory = true, help = "Database name") String name,
            @CliOption(key = "username", mandatory = true, help = "Database user") String username,
            @CliOption(key = "password", mandatory = true, help = "Database password") String password
    ) {
        AmbariDatabaseDetailsJson databaseDetails = new AmbariDatabaseDetailsJson();
        databaseDetails.setVendor(com.sequenceiq.cloudbreak.api.model.DatabaseVendor.valueOf(vendor.getName()));
        databaseDetails.setHost(host);
        databaseDetails.setPort(port);
        databaseDetails.setName(name);
        databaseDetails.setUserName(username);
        databaseDetails.setPassword(password);
        try {
            String error = shellContext.cloudbreakClient().utilEndpoint().testAmbariDatabase(databaseDetails).getError();
            if (error == null) {
                shellContext.setAmbariDatabaseDetailsJson(databaseDetails);
                return com.sequenceiq.cloudbreak.api.model.DatabaseVendor.outOfTheBoxVendors().contains(databaseDetails.getVendor())
                        ? "This type of database supported out of the box, Cloudbreak would initialize it"
                        : "This type of database not supported out of the box, Cloudbreak couldn't initialize it";
            }
            return error;
        } catch (Exception ex) {
            shellContext.resetAmbariDatabaseDetailsJson();
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }
}
