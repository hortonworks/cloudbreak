package com.sequenceiq.cloudbreak.shell.commands.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.shell.commands.BaseCommands;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.model.ShellContext;

public class BlueprintCommands implements BaseCommands {

    private ShellContext shellContext;

    public BlueprintCommands(ShellContext shellContext) {
        this.shellContext = shellContext;
    }

    @CliAvailabilityIndicator(value = "blueprint create")
    public boolean createAvailable() {
        return true;
    }

    @CliCommand(value = "blueprint create", help = "Add a new blueprint with either --url or --file")
    public String create(
            @CliOption(key = "name", mandatory = true, help = "Name of the blueprint to download from") String name,
            @CliOption(key = "description", help = "Description of the blueprint to download from") String description,
            @CliOption(key = "url", help = "URL of the blueprint to download from") String url,
            @CliOption(key = "file", help = "File which contains the blueprint") File file,
            @CliOption(key = "publicInAccount", help = "flags if the blueprint is public in the account",
                    unspecifiedDefaultValue = "false", specifiedDefaultValue = "true") boolean publicInAccount) {
        try {
            String message;
            String json = file == null ? IOUtils.toString(new URL(url)) : IOUtils.toString(new FileInputStream(file));
            if (json != null) {
                BlueprintRequest blueprintRequest = new BlueprintRequest();
                blueprintRequest.setName(name);
                blueprintRequest.setDescription(description);
                blueprintRequest.setAmbariBlueprint(shellContext.objectMapper().readValue(json, JsonNode.class));
                String id;
                if (publicInAccount) {
                    id = shellContext.cloudbreakClient().blueprintEndpoint().postPublic(blueprintRequest).getId().toString();
                } else {
                    id = shellContext.cloudbreakClient().blueprintEndpoint().postPrivate(blueprintRequest).getId().toString();
                }
                shellContext.addBlueprint(id);
                if (shellContext.cloudbreakClient().blueprintEndpoint().getPublics().isEmpty()) {
                    shellContext.setHint(
                            shellContext.isMarathonMode() ? Hints.CONFIGURE_MARATHON_HOSTGROUP : Hints.CONFIGURE_INSTANCEGROUP);
                } else {
                    shellContext.setHint(shellContext.isMarathonMode() ? Hints.CONFIGURE_MARATHON_HOSTGROUP : Hints.SELECT_STACK);
                }
                message = String.format("Blueprint created with id: '%s' and name: '%s'", id, name);
            } else {
                message = "No blueprint specified";
            }
            return message;
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    @CliAvailabilityIndicator(value = "blueprint list")
    public boolean listAvailable() {
        return true;
    }

    @Override
    @CliCommand(value = "blueprint list", help = "Shows the currently available blueprints")
    public String list() throws Exception {
        try {
            Set<BlueprintResponse> publics = shellContext.cloudbreakClient().blueprintEndpoint().getPublics();
            return shellContext.outputTransformer().render(shellContext.responseTransformer().transformToMap(publics, "id", "name"), "ID", "INFO");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @Override
    @CliAvailabilityIndicator(value = { "blueprint select --id", "blueprint select --name" })
    public boolean selectAvailable() {
        return shellContext.isBlueprintAccessible();
    }

    @Override
    public String select(Long id, String name) {
        try {
            if (id != null) {
                if (shellContext.cloudbreakClient().blueprintEndpoint().get(id) != null) {
                    shellContext.addBlueprint(id.toString());
                    shellContext.resetMarathonHostGroups();
                    shellContext.setHint(
                            shellContext.isMarathonMode() ? Hints.CONFIGURE_MARATHON_HOSTGROUP : Hints.CONFIGURE_INSTANCEGROUP);
                    return String.format("Blueprint has been selected, id: %s", id);
                }
            } else if (name != null) {
                BlueprintResponse blueprint = shellContext.cloudbreakClient().blueprintEndpoint().getPublic(name);
                if (blueprint != null) {
                    shellContext.addBlueprint(blueprint.getId().toString());
                    shellContext.resetMarathonHostGroups();
                    shellContext.setHint(
                            shellContext.isMarathonMode() ? Hints.CONFIGURE_MARATHON_HOSTGROUP : Hints.CONFIGURE_INSTANCEGROUP);
                    return String.format("Blueprint has been selected, name: %s", name);
                }
            }
            return "No blueprint specified (select a blueprint by --id or --name)";
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "blueprint select --id", help = "Select the blueprint by its id")
    @Override
    public String selectById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return select(id, null);
    }

    @CliCommand(value = "blueprint select --name", help = "Select the blueprint by its name")
    @Override
    public String selectByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return select(null, name);
    }

    @Override
    @CliAvailabilityIndicator(value = { "blueprint show --id", "blueprint show --name" })
    public boolean showAvailable() {
        return true;
    }

    @Override
    public String show(Long id, String name) {
        try {
            BlueprintResponse blueprintResponse;
            if (id != null) {
                blueprintResponse = shellContext.cloudbreakClient().blueprintEndpoint().get(id);
            } else if (name != null) {
                blueprintResponse = shellContext.cloudbreakClient().blueprintEndpoint().getPublic(name);
            } else {
                return "No blueprints specified.";
            }
            return shellContext.outputTransformer().render(
                    shellContext.responseTransformer().transformObjectToStringMap(blueprintResponse, "ambariBlueprint"), "FIELD", "INFO")
                    + "\n\n"
                    + shellContext.outputTransformer().render(getComponentMap(blueprintResponse.getAmbariBlueprint()), "HOSTGROUP", "COMPONENT");
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "blueprint show --id", help = "Show the blueprint by its id")
    @Override
    public String showById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return show(id, null);
    }

    @CliCommand(value = "blueprint show --name", help = "Show the blueprint by its name")
    @Override
    public String showByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return show(null, name);
    }

    @Override
    @CliAvailabilityIndicator(value = { "blueprint delete --id", "blueprint delete --name" })
    public boolean deleteAvailable() {
        return true;
    }

    @Override
    public String delete(Long id, String name) throws Exception {
        try {
            if (id != null) {
                shellContext.cloudbreakClient().blueprintEndpoint().delete(id);
                return String.format("Blueprint deleted with %s id", id);
            } else if (name != null) {
                shellContext.cloudbreakClient().blueprintEndpoint().deletePublic(name);
                return String.format("Blueprint deleted with %s name", name);
            } else {
                return "No blueprint specified (select a blueprint by --id or --name)";
            }
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException(ex);
        }
    }

    @CliCommand(value = "blueprint delete --id", help = "Delete the blueprint by its id")
    @Override
    public String deleteById(@CliOption(key = "", mandatory = true) Long id) throws Exception {
        return delete(id, null);
    }

    @CliCommand(value = "blueprint delete --name", help = "Delete the blueprint by its name")
    @Override
    public String deleteByName(@CliOption(key = "", mandatory = true) String name) throws Exception {
        return delete(null, name);
    }

    @CliAvailabilityIndicator(value = "blueprint defaults")
    public boolean defaultAvailable() {
        return true;
    }

    @CliCommand(value = "blueprint defaults", help = "Adds the default blueprints to Ambari")
    public String defaults() {
        String message = "Default blueprints added";
        try {
            shellContext.cloudbreakClient().blueprintEndpoint().getPublics();
        } catch (Exception ex) {
            throw shellContext.exceptionTransformer().transformToRuntimeException("Failed to add the default blueprints: " + ex.getMessage());
        }
        return message;
    }

    @Override
    public ShellContext shellContext() {
        return shellContext;
    }

    private Map<String, List<String>> getComponentMap(String json) {
        Map<String, List<String>> map = new HashMap<>();
        try {
            JsonNode hostGroups = shellContext.objectMapper().readTree(json.getBytes()).get("host_groups");
            for (JsonNode hostGroup : hostGroups) {
                List<String> components = new ArrayList<>();
                JsonNode componentsNodes = hostGroup.get("components");
                for (JsonNode componentsNode : componentsNodes) {
                    components.add(componentsNode.get("name").asText());
                }
                map.put(hostGroup.get("name").asText(), components);
            }
        } catch (IOException e) {
            map = new HashMap<>();
        }
        return map;
    }

}
