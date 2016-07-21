package com.sequenceiq.cloudbreak.shell.commands.common;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.RDSConfigJson;
import com.sequenceiq.cloudbreak.api.model.RDSConfigResponse;
import com.sequenceiq.cloudbreak.api.model.RDSDatabase;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class RdsConfigCommands implements BaseCommands {

    private ShellContext shellContext;

    public RdsConfigCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = { "rdsconfig create" })
    public boolean createAvailable() {
        return true;
    }

    @CliCommand(value = "rdsconfig create", help = "Adds a new RDS configuration")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the config") String name,
            @CliOption(key = "databaseType", mandatory = true, help = "Type of the database [POSTGRES or MYSQL]") RDSDatabase databaseType,
            @CliOption(key = "connectionUrl", mandatory = true, help = "JDBC connection URL for the RDS") String connectionUrl,
            @CliOption(key = "connectionUserName", mandatory = true, help = "Username to use for the connection") String connectionUsername,
            @CliOption(key = "connectionPassword", mandatory = true, help = "Password to use for the connection") String connectionPassword,
            @CliOption(key = "hdpVersion", mandatory = true, help = "Compatible HDP version for the RDS configuration") String hdpVersion,
            @CliOption(key = "publicInAccount", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
                    help = "flags if the config is public in the account") Boolean publicInAccount) {
        try {
            RDSConfigJson rdsConfig = new RDSConfigJson();
            rdsConfig.setName(name);
            rdsConfig.setDatabaseType(databaseType);
            rdsConfig.setConnectionURL(connectionUrl);
            rdsConfig.setConnectionUserName(connectionUsername);
            rdsConfig.setConnectionPassword(connectionPassword);
            rdsConfig.setHdpVersion(hdpVersion);
            IdJson id;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().rdsConfigEndpoint().postPublic(rdsConfig);
            } else {
                id = shellContext.cloudbreakClient().rdsConfigEndpoint().postPrivate(rdsConfig);
            }
            return String.format("RDS config created with id: '%d' and name: '%s'", id.getId(), rdsConfig.getName());
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }


    @Override
    @CliAvailabilityIndicator(value = { "rdsconfig select --id", "rdsconfig select --name" })
    public boolean selectAvailable() {
        return shellContext.isRdsConfigAccessible();
    }

    @Override
    public String select(Long id, String name) throws Exception {
        try {
            if (id != null) {
                if (shellContext.cloudbreakClient().rdsConfigEndpoint().get(id) != null) {
                    shellContext.addRdsConfig(id.toString());
                    return String.format("RDS config has been selected, id: %s", id);
                }
            } else if (name != null) {
                RDSConfigResponse config = shellContext.cloudbreakClient().rdsConfigEndpoint().getPublic(name);
                if (config != null) {
                    shellContext.addRdsConfig(config.getId());
                    return String.format("RDS config has been selected, name: %s", name);
                }
            }
            return "No SSSD config specified (select a config by --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "rdsconfig select --id", help = "Selects the RDS config by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }


    @CliCommand(value = "rdsconfig select --name", help = "Selects the RDS config by its name")
    @Override
    public String selectByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return select(null, name);
    }

    @Override
    public boolean showAvailable() {
        return true;
    }

    @Override
    public String show(Long id, String name) throws Exception {
        try {
            RDSConfigResponse response;
            if (id != null) {
                response = shellContext.cloudbreakClient().rdsConfigEndpoint().get(id);
            } else if (name != null) {
                response = shellContext.cloudbreakClient().rdsConfigEndpoint().getPublic(name);
            } else {
                return "RDS config not specified.";
            }
            Map<String, String> map = new HashMap<>();
            map.put("id", response.getId().toString());
            map.put("name", response.getName());
            map.put("databaseType", response.getDatabaseType().toString());
            map.put("connectionUrl", response.getConnectionURL());
            map.put("connectionUsername", response.getConnectionUserName());
            map.put("hdpVersion", response.getHdpVersion());
            return shellContext.outputTransformer().render(map, "FIELD", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "rdsconfig show --id", help = "Show the RDS config by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "rdsconfig show --name", help = "Show the RDS config by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @CliAvailabilityIndicator(value = { "rdsconfig delete --id", "rdsconfig delete --name" })
    @Override
    public boolean deleteAvailable() {
        return true;
    }

    @Override
    public String delete(Long id, String name) throws Exception {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().rdsConfigEndpoint().delete(id);
                return String.format("RDS config deleted with %s id", id);
            } else if (name != null) {
                shellContext.cloudbreakClient().rdsConfigEndpoint().deletePublic(name);
                return String.format("RDS config deleted with %s name", name);
            }
            return "RDS config not specified (select RDS config with --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "rdsconfig delete --id", help = "Deletes the RDS config by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "rdsconfig delete --name", help = "Deletes the RDS config by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @CliAvailabilityIndicator(value = "rdsconfig list")
    @Override
    public boolean listAvailable() {
        return true;
    }

    @CliCommand(value = "rdsconfig list", help = "Shows the currently available RDS configurations")
    @Override
    public String list() {
        try {
            Set<RDSConfigResponse> publics = shellContext.cloudbreakClient().rdsConfigEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }
}
