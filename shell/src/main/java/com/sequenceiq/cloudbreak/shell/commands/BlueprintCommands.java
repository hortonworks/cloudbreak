package com.sequenceiq.cloudbreak.shell.commands;

import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderMultiValueMap;
import static com.sequenceiq.cloudbreak.shell.support.TableRenderer.renderSingleMap;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.core.CommandMarker;
import org.springframework.shell.core.annotation.CliAvailabilityIndicator;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.BlueprintEndpoint;
import com.sequenceiq.cloudbreak.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.shell.model.CloudbreakContext;
import com.sequenceiq.cloudbreak.shell.model.Hints;
import com.sequenceiq.cloudbreak.shell.transformer.ResponseTransformer;

@Component
public class BlueprintCommands implements CommandMarker {

    @Autowired
    private CloudbreakContext context;
    @Autowired
    private BlueprintEndpoint blueprintEndpoint;
    @Autowired
    private ResponseTransformer responseTransformer;
    private final ObjectMapper mapper = new ObjectMapper();

    @CliAvailabilityIndicator(value = "blueprint list")
    public boolean isBlueprintListCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "blueprint select")
    public boolean isBlueprintSelectCommandAvailable() throws Exception {
        return context.isBlueprintAccessible();
    }

    @CliAvailabilityIndicator(value = "blueprint add")
    public boolean isBlueprintAddCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "blueprint show")
    public boolean isBlueprintShowCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "blueprint delete")
    public boolean isBlueprintDeleteCommandAvailable() {
        return true;
    }

    @CliAvailabilityIndicator(value = "blueprint defaults")
    public boolean isBlueprintDefaultsAddCommandAvailable() {
        return true;
    }

    @CliCommand(value = "blueprint defaults", help = "Adds the default blueprints to Ambari")
    public String addBlueprint() {
        String message = "Default blueprints added";
        try {
            blueprintEndpoint.getPublics();
        } catch (Exception e) {
            message = "Failed to add the default blueprints: " + e.getMessage();
        }
        return message;
    }

    @CliCommand(value = "blueprint delete", help = "Delete the blueprint by its id or name")
    public String deleteBlueprint(
            @CliOption(key = "id", mandatory = false, help = "Id of the blueprint") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the blueprint") String name) {
        try {
            if (id != null) {
                blueprintEndpoint.delete(Long.valueOf(id));
                return String.format("Blueprint deleted with %s id", id);
            } else if (name != null) {
                blueprintEndpoint.deletePublic(name);
                return String.format("Blueprint deleted with %s name", name);
            } else {
                return "No blueprint specified (select a blueprint by --id or --name)";
            }
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "blueprint list", help = "Shows the currently available blueprints")
    public String listBlueprints() {
        try {
            Set<BlueprintResponse> publics = blueprintEndpoint.getPublics();
            return renderSingleMap(responseTransformer.transformToMap(publics, "id", "blueprintName"), "ID", "INFO");
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "blueprint show", help = "Shows the blueprint by its id or name")
    public Object showBlueprint(
            @CliOption(key = "id", mandatory = false, help = "Id of the blueprint") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the blueprint") String name) {
        try {
            BlueprintResponse blueprintResponse;
            if (id != null) {
                blueprintResponse = blueprintEndpoint.get(Long.valueOf(id));
            } else if (name != null) {
                blueprintResponse = blueprintEndpoint.getPublic(name);
            } else {
                return "No blueprints specified.";
            }

            return renderSingleMap(responseTransformer.transformObjectToStringMap(blueprintResponse, "ambariBlueprint"), "FIELD", "INFO")
                    + "\n\n" + renderMultiValueMap(getComponentMap(blueprintResponse.getAmbariBlueprint()), "HOSTGROUP", "COMPONENT");
        } catch (Exception ex) {
            return ex.toString();
        }

    }

    @CliCommand(value = "blueprint select", help = "Select the blueprint by its id or name")
    public String selectBlueprint(
            @CliOption(key = "id", mandatory = false, help = "Id of the blueprint") String id,
            @CliOption(key = "name", mandatory = false, help = "Name of the blueprint") String name) {
        try {
            if (id != null) {
                if (blueprintEndpoint.get(Long.valueOf(id)) != null) {
                    context.addBlueprint(id);
                    context.setHint(Hints.CONFIGURE_INSTANCEGROUP);
                    return String.format("Blueprint has been selected, id: %s", id);
                }
            } else if (name != null) {
                BlueprintResponse blueprint = blueprintEndpoint.getPublic(name);
                if (blueprint != null) {
                    context.addBlueprint(blueprint.getId().toString());
                    context.setHint(Hints.CONFIGURE_INSTANCEGROUP);
                    return String.format("Blueprint has been selected, name: %s", name);
                }
            }
            return "No blueprint specified (select a blueprint by --id or --name)";
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    @CliCommand(value = "blueprint add", help = "Add a new blueprint with either --url or --file")
    public String addBlueprint(
            @CliOption(key = "description", mandatory = true, help = "Description of the blueprint to download from") String description,
            @CliOption(key = "name", mandatory = true, help = "Name of the blueprint to download from") String name,
            @CliOption(key = "url", mandatory = false, help = "URL of the blueprint to download from") String url,
            @CliOption(key = "file", mandatory = false, help = "File which contains the blueprint") File file,
            @CliOption(key = "publicInAccount", mandatory = false, help = "flags if the blueprint is public in the account") Boolean publicInAccount) {
        try {
            String message;
            String json = file == null ? IOUtils.toString(new URL(url)) : IOUtils.toString(new FileInputStream(file));
            if (json != null) {
                BlueprintRequest blueprintRequest = new BlueprintRequest();
                blueprintRequest.setName(name);
                blueprintRequest.setDescription(description);
                blueprintRequest.setAmbariBlueprint(mapper.readValue(json, JsonNode.class));
                String id;
                if (publicInAccount) {
                    id = blueprintEndpoint.postPublic(blueprintRequest).getId().toString();
                } else {
                    id = blueprintEndpoint.postPrivate(blueprintRequest).getId().toString();
                }
                context.addBlueprint(id);
                if (blueprintEndpoint.getPublics().isEmpty()) {
                    context.setHint(Hints.CONFIGURE_INSTANCEGROUP);
                } else {
                    context.setHint(Hints.SELECT_STACK);
                }
                message = String.format("Blueprint: '%s' has been added, id: %s", getBlueprintName(json), id);
            } else {
                message = "No blueprint specified";
            }
            return message;
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private String getBlueprintName(String json) {
        String result = "";
        try {
            result = mapper.readTree(json.getBytes()).get("Blueprints").get("blueprint_name").asText();
        } catch (IOException e) {
            e.toString();
        }
        return result;
    }

    private  Map<String, List<String>> getComponentMap(String json) {
        Map<String, List<String>> map = new HashMap<>();
        try {
            JsonNode hostGroups = mapper.readTree(json.getBytes()).get("host_groups");
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
