package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.BlueprintParameterJson;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;
import com.sequenceiq.cloudbreak.domain.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.converter.util.URLUtils;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class JsonToBlueprintConverter extends AbstractConversionServiceAwareConverter<BlueprintRequest, Blueprint> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonToBlueprintConverter.class);

    @Inject
    private JsonHelper jsonHelper;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Override
    public Blueprint convert(BlueprintRequest json) {
        Blueprint blueprint = new Blueprint();
        if (json.getUrl() != null && !json.getUrl().isEmpty()) {
            String sourceUrl = json.getUrl().trim();
            try {
                String urlText = URLUtils.readUrl(sourceUrl);
                jsonHelper.createJsonFromString(urlText);
                blueprint.setBlueprintText(urlText);
            } catch (Exception e) {
                throw new BadRequestException("Cannot download ambari blueprint from: " + sourceUrl, e);
            }
        } else {
            blueprint.setBlueprintText(json.getAmbariBlueprint());
        }
        validateBlueprint(blueprint.getBlueprintText());
        blueprint.setName(json.getName());
        blueprint.setDescription(json.getDescription());
        blueprint.setStatus(ResourceStatus.USER_MANAGED);
        prepareBlueprintInputs(json, blueprint);

        try {
            JsonNode root = JsonUtil.readTree(blueprint.getBlueprintText());
            blueprint.setBlueprintName(blueprintUtils.getBlueprintName(root));
            blueprint.setHostGroupCount(blueprintUtils.countHostGroups(root));
        } catch (IOException e) {
            throw new BadRequestException("Invalid Blueprint: Failed to parse JSON.", e);
        }

        return blueprint;
    }

    private void prepareBlueprintInputs(BlueprintRequest json, Blueprint blueprint) {
        List<BlueprintParameter> blueprintParameterList = new ArrayList<>();
        for (BlueprintParameterJson blueprintParameterJson : json.getInputs()) {
            BlueprintParameter blueprintParameter = new BlueprintParameter();
            blueprintParameter.setReferenceConfiguration(blueprintParameterJson.getReferenceConfiguration());
            blueprintParameter.setDescription(blueprintParameterJson.getDescription());
            blueprintParameter.setName(blueprintParameterJson.getName());
            blueprintParameterList.add(blueprintParameter);
        }
        BlueprintInputParameters inputParameters = new BlueprintInputParameters(blueprintParameterList);
        try {
            blueprint.setInputParameters(new Json(inputParameters));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid Blueprint: Failed to parse inputs.", e);
        }
    }

    public Blueprint convert(String name, String blueprintText, boolean publicInAccount) {
        Blueprint blueprint = new Blueprint();
        blueprint.setName(name);
        blueprint.setBlueprintText(blueprintText);
        blueprint.setPublicInAccount(publicInAccount);
        validateBlueprint(blueprint.getBlueprintText());
        try {
            JsonNode root = JsonUtil.readTree(blueprint.getBlueprintText());
            blueprint.setBlueprintName(blueprintUtils.getBlueprintName(root));
            blueprint.setHostGroupCount(blueprintUtils.countHostGroups(root));
        } catch (IOException e) {
            throw new BadRequestException("Invalid Blueprint: Failed to parse JSON.", e);
        }

        return blueprint;
    }

    private void validateBlueprint(String blueprintText) {
        try {
            JsonNode root = JsonUtil.readTree(blueprintText);
            hasBlueprintInBlueprint(root);
            hasBlueprintNameInBlueprint(root);
            validateHostGroups(root);
        } catch (IOException e) {
            throw new BadRequestException("Invalid Blueprint: Failed to parse JSON.", e);
        }
    }

    private void validateHostGroups(JsonNode root) {
        JsonNode hostGroups = root.path("host_groups");
        if (hostGroups.isMissingNode() || !hostGroups.isArray() || hostGroups.size() == 0) {
            throw new BadRequestException("Invalid blueprint: 'host_groups' node is missing from JSON or is not an array or empty.");
        }
        for (JsonNode hostGroup : hostGroups) {
            JsonNode hostGroupName = hostGroup.path("name");
            if (hostGroupName.isMissingNode() || !hostGroupName.isTextual() || hostGroupName.asText().isEmpty()) {
                throw new BadRequestException("Invalid blueprint: one of the 'host_groups' has no name.");
            }
            validateComponentsInHostgroup(hostGroup, hostGroupName.asText());
        }
    }

    private void validateComponentsInHostgroup(JsonNode hostGroup, String hostGroupName) {
        JsonNode components = hostGroup.path("components");
        if (components.isMissingNode() || !components.isArray() || components.size() == 0) {
            throw new BadRequestException(
                    String.format("Invalid blueprint: '%s' hostgroup's 'components' node is missing from JSON or is not an array or empty.", hostGroupName));
        }
        for (JsonNode component : components) {
            JsonNode componentName = component.path("name");
            if (componentName.isMissingNode() || !componentName.isTextual() || componentName.asText().isEmpty()) {
                throw new BadRequestException(String.format("Invalid blueprint: one fo the 'components' has no name in '%s' hostgroup.", hostGroupName));
            }
        }
    }

    private void hasBlueprintNameInBlueprint(JsonNode root) {
        if (root.path("Blueprints").path("blueprint_name").isMissingNode()) {
            throw new BadRequestException("Invalid blueprint: 'blueprint_name' under 'Blueprints' is missing from JSON.");
        }
    }

    private void hasBlueprintInBlueprint(JsonNode root) {
        if (root.path("Blueprints").isMissingNode()) {
            throw new BadRequestException("Invalid blueprint: 'Blueprints' node is missing from JSON.");
        }
    }
}
