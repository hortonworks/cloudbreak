package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.SssdConfigRequest;
import com.sequenceiq.cloudbreak.api.model.SssdConfigResponse;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.cloudbreak.shell.completion.SssdProviderType;
import com.sequenceiq.cloudbreak.shell.completion.SssdSchemaType;
import com.sequenceiq.cloudbreak.shell.completion.SssdTlsReqcertType;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.transformer.ExceptionTransformer;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class SssdConfigCommands implements CommandMarker {

    @Inject
    private CloudbreakContext context;
    @Inject
    private CloudbreakClient cloudbreakClient;
    @Inject
    private ResponseTransformer responseTransformer;
    @Inject
    private ExceptionTransformer exceptionTransformer;

    @CliAvailabilityIndicator(value = "sssdconfig list")
    public boolean isSssdConfigListCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "sssdconfig select")
    public boolean isSssdConfigSelectCommandAvailable() throws Exception {
        return context.isSssdConfigAccessible();
    }

    @CliAvailabilityIndicator(value = "sssdconfig add")
    public boolean isAddCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "sssdconfig show")
    public boolean isShowCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "sssdconfig delete")
    public boolean isDeleteCommandAvailable() {
        return true;
    }

    @CliCommand(value = "sssdconfig list", help = "Shows the currently available configs")
    public String list() {
        try {
            return renderSingleMap(responseTransformer.transformToMap(cloudbreakClient.sssdConfigEndpoint().getPublics(), "id", "name"), true, "ID", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "sssdconfig select", help = "Select the config by its id or name")
    public String select(
            @CliOption(key = "id", mandatory = false, help = "Id of the config") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the config") String name) {
        try {
            if (id != null) {
                if (cloudbreakClient.sssdConfigEndpoint().get(Long.valueOf(id)) != null) {
                    context.addSssdConfig(id);
                    return String.format("SSSD config has been selected, id: %s", id);
                }
            } else if (name != null) {
                SssdConfigResponse config = cloudbreakClient.sssdConfigEndpoint().getPublic(name);
                if (config != null) {
                    context.addSssdConfig(config.getId().toString());
                    return String.format("SSSD config has been selected, name: %s", name);
                }
            }
            return "No SSSD config specified (select a config by --id or --name)";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "sssdconfig add", help = "Add a new config")
    public String add(
            @CliOption(key = "name", mandatory = true, help = "Name of the config") String name,
            @CliOption(key = "description", help = "Description of the config") String description,
            @CliOption(key = "providerType", mandatory = true, help = "Type of the provider") SssdProviderType providerType,
            @CliOption(key = "url", mandatory = true, help = "comma-separated list of URIs of the LDAP servers") String url,
            @CliOption(key = "schema", mandatory = true, help = "Schema of the database") SssdSchemaType schema,
            @CliOption(key = "baseSearch", mandatory = true, help = "Search base of the database") String baseSearch,
            @CliOption(key = "tlsReqcert", mandatory = true, unspecifiedDefaultValue = "hard", specifiedDefaultValue = "hard",
                    help = "TLS behavior of connection") SssdTlsReqcertType tlsReqcert,
            @CliOption(key = "adServer", mandatory = false, help = "comma-separated list of IP addresses or hostnames of the AD servers") String adServer,
            @CliOption(key = "kerberosServer", mandatory = false,
                    help = "comma-separated list of IP addresses or hostnames of the Kerberos servers") String kerberosServer,
            @CliOption(key = "kerberosRealm", mandatory = false, help = "name of the Kerberos realm") String kerberosRealm,
            @CliOption(key = "publicInAccount", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
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
                id = cloudbreakClient.sssdConfigEndpoint().postPublic(request);
            } else {
                id = cloudbreakClient.sssdConfigEndpoint().postPrivate(request);
            }
            return String.format("SSSD config created with id: '%d' and name: '%s'", id.getId(), request.getName());
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "sssdconfig upload", help = "Upload a new config")
    public String upload(
            @CliOption(key = "name", mandatory = true, help = "Name of the config") String name,
            @CliOption(key = "description", help = "Description of the config") String description,
            @CliOption(key = "file", mandatory = true, help = "Path of the configuration file") File configFile,
            @CliOption(key = "publicInAccount", mandatory = false, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
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
                id = cloudbreakClient.sssdConfigEndpoint().postPublic(request);
            } else {
                id = cloudbreakClient.sssdConfigEndpoint().postPrivate(request);
            }
            return String.format("SSSD config created with id: '%d' and name: '%s'", id.getId(), request.getName());
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "sssdconfig show", help = "Shows the properties of the specified config")
    public Object show(
            @CliOption(key = "id", mandatory = false, help = "Id of the config") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the config") String name) {
        try {
            SssdConfigResponse response;
            if (id != null) {
                response = cloudbreakClient.sssdConfigEndpoint().get(Long.valueOf(id));
            } else if (name != null) {
                response = cloudbreakClient.sssdConfigEndpoint().getPublic(name);
            } else {
                return "SSSD config not specified.";
            }
            Map<String, String> map = new HashMap<>();
            map.put("id", response.getId().toString());
            map.put("name", response.getName());
            map.put("description", response.getDescription());
            return renderSingleMap(map, "FIELD", "INFO");
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "sssdconfig delete", help = "Delete the config by its id or name")
    public Object delete(
            @CliOption(key = "id", mandatory = false, help = "Id of the config") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the config") String name) {
        try {
            if (id != null) {
                cloudbreakClient.sssdConfigEndpoint().delete(Long.valueOf(id));
                return String.format("SSSD config deleted with %s id", id);
            } else if (name != null) {
                cloudbreakClient.sssdConfigEndpoint().deletePublic(name);
                return String.format("SSSD config deleted with %s name", name);
            }
            return "SSSD config not specified (select sssd by --id or --name)";
        } catch (Exception ex) {
            throw exceptionTransformer.transformToRuntimeException(ex);
        }
    }
}
