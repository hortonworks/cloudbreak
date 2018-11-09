package com.sequenceiq.cloudbreak.converter;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.util.URLUtils;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.json.CloudbreakApiException;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintRequestToBlueprintConverter extends AbstractConversionServiceAwareConverter<BlueprintRequest, Blueprint> {

    private static final String JSON_PARSE_EXCEPTION_MESSAGE = "Invalid Blueprint: Failed to parse JSON.";

    @Inject
    private JsonHelper jsonHelper;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public Blueprint convert(BlueprintRequest json) {
        Blueprint blueprint = new Blueprint();
        if (StringUtils.isNoneEmpty(json.getUrl())) {
            String sourceUrl = json.getUrl().trim();
            try {
                String urlText = URLUtils.readUrl(sourceUrl);
                jsonHelper.createJsonFromString(urlText);
                blueprint.setBlueprintText(urlText);
            } catch (IOException | CloudbreakApiException e) {
                throw new BadRequestException(String.format("Cannot download ambari validation from: %s", sourceUrl), e);
            }
        } else {
            blueprint.setBlueprintText(json.getAmbariBlueprint());
        }
        validateBlueprint(blueprint.getBlueprintText().getRaw());
        JsonNode blueprintJson;
        try {
            blueprintJson = JsonUtil.readTree(blueprint.getBlueprintText().getRaw());
            validateBlueprintStackVersion(blueprintJson);
            setAmbariNameAndHostGrouCount(blueprint, blueprintJson);
        } catch (IOException e) {
            throw new BadRequestException(JSON_PARSE_EXCEPTION_MESSAGE, e);
        }
        blueprint.setName(getNameByItsAvailability(json.getName()));
        blueprint.setDescription(json.getDescription());
        blueprint.setStatus(ResourceStatus.USER_MANAGED);
        blueprint.setTags(createJsonFromTagsMap(json.getTags()));

        return blueprint;
    }

    private void validateBlueprintStackVersion(JsonNode blueprintJson) {
        String stackVersion = blueprintUtils.getBlueprintStackVersion(blueprintJson);
        if (StringUtils.isBlank(stackVersion) || !stackVersion.matches("[0-9]+\\.[0-9]+")) {
            throw new BadRequestException(String.format("Stack version [%s] is not valid. Valid stack version is in MAJOR.MINOR format eg.: 2.6",
                    stackVersion));
        }
    }

    private String getNameByItsAvailability(@Nullable String name) {
        return Strings.isNullOrEmpty(name) ? missingResourceNameGenerator.generateName(APIResourceType.BLUEPRINT) : name;
    }

    private void setAmbariNameAndHostGrouCount(Blueprint blueprint, JsonNode blueprintJson) {
        blueprint.setAmbariName(blueprintUtils.getBlueprintName(blueprintJson));
        blueprint.setHostGroupCount(blueprintUtils.countHostGroups(blueprintJson));
        blueprint.setStackType(blueprintUtils.getBlueprintStackName(blueprintJson));
        blueprint.setStackVersion(blueprintUtils.getBlueprintStackVersion(blueprintJson));
    }

    private Json createJsonFromTagsMap(Map<String, Object> tags) {
        try {
            return new Json(tags);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid tag(s) in the Blueprint: Unable to parse JSON.", e);
        }
    }

    private void validateBlueprint(String blueprintText) {
        try {
            JsonNode root = JsonUtil.readTree(blueprintText);
            hasBlueprintInBlueprint(root);
            hasBlueprintNameInBlueprint(root);
            validateHostGroups(root);
        } catch (IOException e) {
            throw new BadRequestException(JSON_PARSE_EXCEPTION_MESSAGE, e);
        }
    }

    private void validateHostGroups(JsonNode root) {
        JsonNode hostGroups = root.path("host_groups");
        if (hostGroups.isMissingNode() || !hostGroups.isArray() || hostGroups.size() == 0) {
            throw new BadRequestException("Validation error: 'host_groups' node is missing from JSON or is not an array or empty.");
        }
        for (JsonNode hostGroup : hostGroups) {
            JsonNode hostGroupName = hostGroup.path("name");
            if (isTextPropertyMissing(hostGroupName)) {
                throw new BadRequestException("Validation error: one of the 'host_groups' has no name.");
            } else if (!blueprintUtils.isValidHostGroupName(hostGroupName.asText())) {
                throw new BadRequestException(String.format("Validation error: '%s' is not a valid host group name. Host group name "
                        + "must be alphanumeric with underscores ('_').", hostGroupName.asText()));
            }
            validateComponentsInHostgroup(hostGroup, hostGroupName.asText());
        }
    }

    private void validateComponentsInHostgroup(JsonNode hostGroup, String hostGroupName) {
        JsonNode components = hostGroup.path("components");
        if (components.isMissingNode() || !components.isArray() || components.size() == 0) {
            throw new BadRequestException(
                    String.format("Validation error: '%s' hostgroup's 'components' node is missing from JSON or is not an array or empty.", hostGroupName));
        }
        for (JsonNode component : components) {
            JsonNode componentName = component.path("name");
            if (isTextPropertyMissing(componentName)) {
                throw new BadRequestException(String.format("Validation error: one fo the 'components' has no name in '%s' hostgroup.", hostGroupName));
            }
        }
    }

    private boolean isTextPropertyMissing(JsonNode hostGroupName) {
        return hostGroupName.isMissingNode() || !hostGroupName.isTextual() || hostGroupName.asText().isEmpty();
    }

    private void hasBlueprintNameInBlueprint(TreeNode root) {
        if (root.path("Blueprints").path("blueprint_name").isMissingNode()) {
            throw new BadRequestException("Validation error: 'blueprint_name' under 'Blueprints' is missing from JSON.");
        }
    }

    private void hasBlueprintInBlueprint(TreeNode root) {
        if (root.path("Blueprints").isMissingNode()) {
            throw new BadRequestException("Validation error: 'Blueprints' node is missing from JSON.");
        }
    }
}
