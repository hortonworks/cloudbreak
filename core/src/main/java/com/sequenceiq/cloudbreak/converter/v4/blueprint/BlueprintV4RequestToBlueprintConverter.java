package com.sequenceiq.cloudbreak.converter.v4.blueprint;

import java.io.IOException;
import java.util.Map;

import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.template.domain.GeneratedCmTemplate;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.json.CloudbreakApiException;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.util.URLUtils;

@Component
public class BlueprintV4RequestToBlueprintConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintV4RequestToBlueprintConverter.class);

    private static final String JSON_PARSE_EXCEPTION_MESSAGE = "Invalid cluster template: Failed to parse JSON.";

    @Inject
    private JsonHelper jsonHelper;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private ResourceNameGenerator resourceNameGenerator;

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    public Blueprint convert(BlueprintV4Request json) {
        Blueprint blueprint = new Blueprint();
        if (StringUtils.isNotEmpty(json.getUrl())) {
            String sourceUrl = json.getUrl().trim();
            try {
                String urlText = URLUtils.readUrl(sourceUrl);
                jsonHelper.createJsonFromString(urlText);
                blueprint.setBlueprintText(urlText);
            } catch (IOException | CloudbreakApiException e) {
                throw new BadRequestException(String.format("Cannot download cluster template from: %s", sourceUrl), e);
            }
        } else if (!CollectionUtils.isEmpty(json.getServices()) && !Strings.isNullOrEmpty(json.getPlatform())) {
            GeneratedCmTemplate generatedCmTemplate = clusterTemplateGeneratorService.generateTemplateByServices(json.getServices(), json.getPlatform());
            blueprint.setBlueprintText(generatedCmTemplate.getTemplate());
        } else {
            blueprint.setBlueprintText(json.getBlueprint());
        }

        try {
            JsonNode blueprintJson = JsonUtil.readTree(blueprint.getBlueprintJsonText());
            if (blueprintUtils.isBuiltinBlueprint(blueprintJson)) {
                LOGGER.info("Built-in blueprint format detected, applying embedded \"blueprint\" content");
                blueprintJson = blueprintUtils.getBuiltinBlueprintContent(blueprintJson);
                blueprint.setBlueprintText(JsonUtil.writeValueAsString(blueprintJson));
            }

            if (blueprintUtils.isClouderaManagerClusterTemplate(blueprintJson)) {
                blueprint.setStackName(blueprintUtils.getCDHDisplayName(blueprintJson));
                blueprint.setHostGroupCount(blueprintUtils.countHostTemplates(blueprintJson));
                blueprint.setStackVersion(blueprintUtils.getCDHStackVersion(blueprintJson));
                blueprint.setBlueprintUpgradeOption(blueprintUtils.getBlueprintUpgradeOptionForEnabled(blueprintJson));
                blueprint.setHybridOption(blueprintUtils.getHybridOption(blueprintJson));
                blueprint.setStackType("CDH");
            } else {
                throw new BadRequestException("Failed to determine cluster template format");
            }
        } catch (IOException e) {
            throw new BadRequestException(JSON_PARSE_EXCEPTION_MESSAGE, e);
        }
        blueprint.setName(getNameByItsAvailability(json.getName()));
        blueprint.setDescription(json.getDescription());
        blueprint.setStatus(ResourceStatus.USER_MANAGED);
        blueprint.setTags(createJsonFromTagsMap(json.getTags()));
        return blueprint;
    }

    private String getNameByItsAvailability(@Nullable String name) {
        return Strings.isNullOrEmpty(name) ? resourceNameGenerator.generateName(APIResourceType.BLUEPRINT) : name;
    }

    private Json createJsonFromTagsMap(Map<String, Object> tags) {
        try {
            return new Json(tags);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid tag(s) in the cluster template: Unable to parse JSON.", e);
        }
    }
}