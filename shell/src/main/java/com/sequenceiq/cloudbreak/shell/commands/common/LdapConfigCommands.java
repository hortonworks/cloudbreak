package com.sequenceiq.cloudbreak.shell.commands.common;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.sequenceiq.cloudbreak.api.model.IdJson;
import com.sequenceiq.cloudbreak.api.model.LdapConfigRequest;
import com.sequenceiq.cloudbreak.api.model.LdapConfigResponse;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class LdapConfigCommands implements BaseCommands {

    private ShellContext shellContext;

    public LdapConfigCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = { "ldapconfig create" })
    public boolean createAvailable() {
        return true;
    }

    @CliCommand(value = "ldapconfig create", help = "Adds a new Ldap configuration")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the config") String name,
            @CliOption(key = "description", help = "Description of the config") String description,
            @CliOption(key = "serverHost", mandatory = true, help = "Public host or IP address of LDAP server") String serverHost,
            @CliOption(key = "serverPort", mandatory = true, help = "Port of LDAP server (typically: 389 or 636 for LDAPS)") Integer serverPort,
            @CliOption(key = "serverSSL", mandatory = true, unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
                    help = "Determines if LDAP or LDAP over SSL is to be used") Boolean serverSSL,
            @CliOption(key = "bindDn", mandatory = true,
                    help = "Bind distinguished name for connection test and group search (e.g. cn=admin,dc=example,dc=org)") String bindDn,
            @CliOption(key = "bindPassword", mandatory = true, help = "password for the provided bind DN") String bindPassword,
            @CliOption(key = "userSearchBase", mandatory = true,
                    help = "template for user search for authentication (e.g. dc=hadoop,dc=apache,dc=org)") String userSearchBase,
            @CliOption(key = "userSearchFilter",
                    help = "filter for user search for authentication (e.g. (&amp;(objectclass=person)(sAMAccountName={2})) )") String userSearchFilter,
            @CliOption(key = "groupSearchBase",
                    help = "template for group search for authorization (e.g. dc=hadoop,dc=apache,dc=org)") String groupSearchBase,
            @CliOption(key = "groupSearchFilter",  help = "filter for group search for authorization") String groupSearchFilter,
            @CliOption(key = "principalRegex", help = "parses the principal for insertion into templates via regex.") String principalRegex,

            @CliOption(key = "publicInAccount", unspecifiedDefaultValue = "false", specifiedDefaultValue = "true",
                    help = "flags if the config is public in the account") Boolean publicInAccount) {
        try {
            LdapConfigRequest config = new LdapConfigRequest();
            config.setName(name);
            config.setDescription(description);
            config.setBindDn(bindDn);
            config.setBindPassword(bindPassword);
            config.setServerHost(serverHost);
            config.setServerPort(serverPort);
            config.setServerSSL(serverSSL);
            config.setGroupSearchBase(groupSearchBase);
            config.setGroupSearchFilter(groupSearchFilter);
            config.setUserSearchBase(userSearchBase);
            config.setUserSearchFilter(userSearchFilter);
            config.setPrincipalRegex(principalRegex);
            IdJson id;
            if (publicInAccount) {
                id = shellContext.cloudbreakClient().ldapConfigEndpoint().postPublic(config);
            } else {
                id = shellContext.cloudbreakClient().ldapConfigEndpoint().postPrivate(config);
            }
            return String.format("Ldap config created with id: '%d' and name: '%s'", id.getId(), config.getName());
        } catch (Exception e) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(e);
        }
    }

    @Override
    @CliAvailabilityIndicator(value = { "ldapconfig select --id", "ldapconfig select --name" })
    public boolean selectAvailable() {
        return true;
    }

    @Override
    public String select(Long id, String name) throws Exception {
        try {
            if (id != null) {
                if (shellContext.cloudbreakClient().ldapConfigEndpoint().get(id) != null) {
                    shellContext.addLdapConfig(id.toString());
                    return String.format("Ldap config has been selected, id: %s", id);
                }
            } else if (name != null) {
                LdapConfigResponse config = shellContext.cloudbreakClient().ldapConfigEndpoint().getPublic(name);
                if (config != null) {
                    shellContext.addLdapConfig(config.getId().toString());
                    return String.format("Ldap config has been selected, name: %s", name);
                }
            }
            return "No LDAP config specified (select a config by --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "ldapconfig select --id", help = "Selects the Ldap config by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "ldapconfig select --name", help = "Selects the Ldap config by its name")
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
            LdapConfigResponse response;
            if (id != null) {
                response = shellContext.cloudbreakClient().ldapConfigEndpoint().get(id);
            } else if (name != null) {
                response = shellContext.cloudbreakClient().ldapConfigEndpoint().getPublic(name);
            } else {
                return "Ldap config not specified.";
            }

            Map<String, String> map = new HashMap<>();
            map.put("id", response.getId().toString());
            map.put("name", response.getName());
            map.put("serverHost", response.getServerHost());
            map.put("serverPort", response.getServerPort().toString());
            map.put("serverSSL", response.getServerSSL().toString());
            map.put("bindDn", response.getBindDn());
            map.put("bindPassword", response.getBindPassword());
            map.put("userSearchBase", response.getUserSearchBase());
            map.put("userSearchFilter", response.getUserSearchFilter());
            map.put("groupSearchBase", response.getGroupSearchBase());
            map.put("groupSearchFilter", response.getGroupSearchFilter());
            //map.put("principalRegex", response.getPrincipalRegex());
            //return shellContext.outputTransformer().render(shellContext.responseTransformer().transformObjectToStringMap(response), "FIELD", "VALUE");
            return shellContext.outputTransformer().render(map, "FIELD", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "ldapconfig show --id", help = "Show the Ldap config by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "ldapconfig show --name", help = "Show the Ldap config by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @CliAvailabilityIndicator(value = { "ldapconfig delete --id", "ldapconfig delete --name" })
    @Override
    public boolean deleteAvailable() {
        return true;
    }

    @Override
    public String delete(Long id, String name) throws Exception {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().ldapConfigEndpoint().delete(id);
                return String.format("Ldap config deleted with %s id", id);
            } else if (name != null) {
                shellContext.cloudbreakClient().ldapConfigEndpoint().deletePublic(name);
                return String.format("Ldap config deleted with %s name", name);
            }
            return "Ldap config not specified (select Ldap config with --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "ldapconfig delete --id", help = "Deletes the Ldap config by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "ldapconfig delete --name", help = "Deletes the Ldap config by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @CliAvailabilityIndicator(value = "ldapconfig list")
    @Override
    public boolean listAvailable() {
        return true;
    }

    @CliCommand(value = "ldapconfig list", help = "Shows the currently available Ldap configurations")
    @Override
    public String list() {
        try {
            Set<LdapConfigResponse> publics = shellContext.cloudbreakClient().ldapConfigEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "NAME");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }
}