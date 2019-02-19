package com.sequenceiq.cloudbreak.converter.v4.clusterdefinition;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.AmbariBlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.converter.util.URLUtils;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.json.CloudbreakApiException;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class ClusterDefinitionV4RequestToClusterDefinitionConverter
        extends AbstractConversionServiceAwareConverter<ClusterDefinitionV4Request, ClusterDefinition> {

    private static final String JSON_PARSE_EXCEPTION_MESSAGE = "Invalid cluster definition: Failed to parse JSON.";

    @Inject
    private JsonHelper jsonHelper;

    @Inject
    private AmbariBlueprintUtils ambariBlueprintUtils;

    @Inject
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Override
    public ClusterDefinition convert(ClusterDefinitionV4Request json) {
        ClusterDefinition clusterDefinition = new ClusterDefinition();
        if (StringUtils.isNoneEmpty(json.getUrl())) {
            String sourceUrl = json.getUrl().trim();
            try {
                String urlText = URLUtils.readUrl(sourceUrl);
                jsonHelper.createJsonFromString(urlText);
                clusterDefinition.setClusterDefinitionText(urlText);
            } catch (IOException | CloudbreakApiException e) {
                throw new BadRequestException(String.format("Cannot download ambari validation from: %s", sourceUrl), e);
            }
        } else {
            clusterDefinition.setClusterDefinitionText(json.getClusterDefinition());
        }
        try {
            JsonNode clusterDefinitionJson = JsonUtil.readTree(clusterDefinition.getClusterDefinitionText());
            if (ambariBlueprintUtils.isAmbariBlueprint(clusterDefinitionJson)) {
                validateAmbariBlueprint(clusterDefinitionJson);
                validateAmbariBlueprintStackVersion(clusterDefinitionJson);
                clusterDefinition.setStackName(ambariBlueprintUtils.getBlueprintName(clusterDefinitionJson));
                clusterDefinition.setHostGroupCount(ambariBlueprintUtils.countHostGroups(clusterDefinitionJson));
                clusterDefinition.setStackType(ambariBlueprintUtils.getBlueprintStackName(clusterDefinitionJson));
                clusterDefinition.setStackVersion(ambariBlueprintUtils.getBlueprintStackVersion(clusterDefinitionJson));
            } else {
                clusterDefinition.setStackName(ambariBlueprintUtils.getCDHDisplayName(clusterDefinitionJson));
                clusterDefinition.setHostGroupCount(ambariBlueprintUtils.countHostTemplates(clusterDefinitionJson));
                clusterDefinition.setStackVersion(ambariBlueprintUtils.getCDHStackVersion(clusterDefinitionJson));
                clusterDefinition.setStackType("CDH");
            }
        } catch (IOException e) {
            throw new BadRequestException(JSON_PARSE_EXCEPTION_MESSAGE, e);
        }
        clusterDefinition.setName(getNameByItsAvailability(json.getName()));
        clusterDefinition.setDescription(json.getDescription());
        clusterDefinition.setStatus(ResourceStatus.USER_MANAGED);
        clusterDefinition.setTags(createJsonFromTagsMap(json.getTags()));
        return clusterDefinition;
    }

    private void validateAmbariBlueprintStackVersion(JsonNode blueprintJson) {
        String stackVersion = ambariBlueprintUtils.getBlueprintStackVersion(blueprintJson);
        if (StringUtils.isBlank(stackVersion) || !stackVersion.matches("[0-9]+\\.[0-9]+")) {
            throw new BadRequestException(String.format("Stack version [%s] is not valid. Valid stack version is in MAJOR.MINOR format eg.: 2.6",
                    stackVersion));
        }
    }

    private String getNameByItsAvailability(@Nullable String name) {
        return Strings.isNullOrEmpty(name) ? missingResourceNameGenerator.generateName(APIResourceType.CLUSTER_DEFINITION) : name;
    }

    private Json createJsonFromTagsMap(Map<String, Object> tags) {
        try {
            return new Json(tags);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid tag(s) in the cluster definition: Unable to parse JSON.", e);
        }
    }

    private void validateAmbariBlueprint(JsonNode root) {
        hasBlueprintInBlueprint(root);
        hasBlueprintNameInBlueprint(root);
        validateHostGroups(root);
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
            } else if (!ambariBlueprintUtils.isValidHostGroupName(hostGroupName.asText())) {
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
