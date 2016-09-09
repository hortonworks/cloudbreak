package com.sequenceiq.cloudbreak.shell.commands.common;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest;
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.completion.SssdProviderType;
import com.sequenceiq.cloudbreak.shell.completion.SssdSchemaType;
import com.sequenceiq.cloudbreak.shell.completion.SssdTlsReqcertType;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class SssdConfigCommands implements BaseCommands {

    private ShellContext shellContext;

    public SssdConfigCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = "sssdconfig list")
    @Override
    public boolean listAvailable() {
        return true;
    }

    @CliCommand(value = "sssdconfig list", help = "Shows the currently available configs")
    @Override
    public String list() {
        try {
            Set<SssdConfigResponse> publics = shellContext.cloudbreakClient().sssdConfigEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "sssdconfig select --id", "sssdconfig select --name" })
    @Override
    public boolean selectAvailable() {
        return shellContext.isSssdConfigAccessible();
    }

    @CliCommand(value = "sssdconfig select --id", help = "Delete the config by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "sssdconfig select --name", help = "Delete the config by its name")
    @Override
    public String selectByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return select(null, name);
    }

    @Override
    public String select(Long id, String name) {
        try {
            if (id != null) {
                if (shellContext.cloudbreakClient().sssdConfigEndpoint().get(id) != null) {
                    shellContext.addSssdConfig(id.toString());
                    return String.format("SSSD config has been selected, id: %s", id);
                }
            } else if (name != null) {
                SssdConfigResponse config = shellContext.cloudbreakClient().sssdConfigEndpoint().getPublic(name);
                if (config != null) {
                    shellContext.addSssdConfig(config.getId().toString());
                    return String.format("SSSD config has been selected, name: %s", name);
                }
            }
            return "No SSSD config specified (select a config by --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "sssdconfig create --withParameters", "sssdconfig create --withFile" })
    public boolean createAvailable() {
        return true;
    }

    @CliCommand(value = "sssdconfig create --withParameters", help = "Add a new config")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the config") String name,
            @CliOption(key = "description", help = "Description of the config") String description,
            @CliOption(key = "providerType", mandatory = true, help = "Type of the provider") SssdProviderType providerType,
            @CliOption(key = "url", mandatory = true, help = "comma-separated list of URIs of the LDAP servers") String url,
            @CliOption(key = "schema", mandatory = true, help = "Schema of the database") SssdSchemaType schema,
            @CliOption(key = "baseSearch", mandatory = true, help = "Search base of the database") String baseSearch,
            @CliOption(key = "tlsReqcert", mandatory = true, unspecifiedDefaultValue = "hard", specifiedDefaultValue = "hard",
                    help = "TLS behavior of connection") SssdTlsReqcertType tlsReqcert,
            @CliOption(key = "adServer", help = "comma-separated list of IP addresses or hostnames of the AD servers") String adServer,
            @CliOption(key = "kerberosServer", help = "comma-separated list of IP addresses or hostnames of the Kerberos servers") String kerberosServer,
            @CliOption(key = "kerberosRealm", help = "name of the Kerberos realm") String kerberosRealm,
            @CliOption(key = "publicInAccount", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
                    help = "flags if the config is public in the account") Boolean publicInAccount) {
        try {
            SssdConfigRequest request = new SssdConfigRequest();
            request.setName(name);
            request.setDescription(description);
            request.setProviderType(com.sequenceiq.cloudbreak.api.model.SssdProviderType.valueOf(providerType.getName()));
            request.setUrl(url);
            request.setSchema(com.sequenceiq.cloudbreak.api.model.SssdSchemaType.valueOf(schema.getName()));
            request.setBaseSearch(baseSearch);
            request.setTlsReqcert(com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType.valueOf(tlsReqcert.getName()));
            request.setAdServer(adServer);
            request.setKerberosServer(kerberosServer);
            request.setKerberosRealm(kerberosRealm);
            IdJson id;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().sssdConfigEndpoint().postPublic(request);
            } else {
                id = shellContext.cloudbreakClient().sssdConfigEndpoint().postPrivate(request);
            }
            return String.format("SSSD config created with id: '%d' and name: '%s'", id.getId(), request.getName());
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "sssdconfig create --withFile", help = "Upload a new config")
    public String upload(
            @CliOption(key = "name", mandatory = true, help = "Name of the config") String name,
            @CliOption(key = "description", help = "Description of the config") String description,
            @CliOption(key = "file", mandatory = true, help = "Path of the configuration file") File configFile,
            @CliOption(key = "publicInAccount", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
                    help = "flags if the config is public in the account") Boolean publicInAccount) {
        try {
            if (configFile != null && !configFile.exists()) {
                return "Configuration file not exists.";
            }
            SssdConfigRequest request = new SssdConfigRequest();
            request.setName(name);
            request.setDescription(description);
            String config = IOUtils.toString(new FileInputStream(configFile));
            request.setConfiguration(config);
            IdJson id;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().sssdConfigEndpoint().postPublic(request);
            } else {
                id = shellContext.cloudbreakClient().sssdConfigEndpoint().postPrivate(request);
            }
            return String.format("SSSD config created with id: '%d' and name: '%s'", id.getId(), request.getName());
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliAvailabilityIndicator(value = { "sssdconfig show --id", "sssdconfig show --name" })
    @Override
    public boolean showAvailable() {
        return true;
    }

    @Override
    public String show(Long id, String name) {
        try {
            SssdConfigResponse response;
            if (id != null) {
                response = shellContext.cloudbreakClient().sssdConfigEndpoint().get(id);
            } else if (name != null) {
                response = shellContext.cloudbreakClient().sssdConfigEndpoint().getPublic(name);
            } else {
                return "SSSD config not specified.";
            }
            Map<String, String> map = new HashMap<>();
            map.put("id", response.getId().toString());
            map.put("name", response.getName());
            map.put("description", response.getDescription());
            return shellContext.outputTransformer().render(map, "FIELD", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "sssdconfig show --id", help = "Show the config by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "sssdconfig show --name", help = "Show the config by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @CliAvailabilityIndicator(value = { "sssdconfig delete --id", "sssdconfig delete --name" })
    @Override
    public boolean deleteAvailable() {
        return true;
    }

    @Override
    public String delete(Long id, String name) {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().sssdConfigEndpoint().delete(id);
                return String.format("SSSD config deleted with %s id", id);
            } else if (name != null) {
                shellContext.cloudbreakClient().sssdConfigEndpoint().deletePublic(name);
                return String.format("SSSD config deleted with %s name", name);
            }
            return "SSSD config not specified (select sssd by --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "sssdconfig delete --id", help = "Delete the config by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "sssdconfig delete --name", help = "Delete the config by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

}
