package com.sequenceiq.cloudbreak.init.blueprint;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;

@Component
@Scope("prototype")
public class DefaultBlueprintCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintCache.class);

    private final Map<String, BlueprintFile> defaultBlueprints = new HashMap<>();

    @Inject
    private BlueprintEntities blueprintEntities;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private BlueprintV4RequestToBlueprintConverter converter;

    @Inject
    private ProviderPreferencesService preferencesService;

    @Inject
    private CommonGovService commonGovService;

    @Inject
    private GovCloudExclusionFilter govCloudExculsionFilter;

    @PostConstruct
    public void loadBlueprintsFromFile() {
        Map<String, Set<String>> blueprints = blueprints();
        boolean govCloudDeployment = commonGovService.govCloudDeployment(
                preferencesService.enabledGovPlatforms(),
                preferencesService.enabledPlatforms());
        for (Map.Entry<String, Set<String>> blueprintEntry : blueprints.entrySet()) {
            try {
                for (String blueprintText : blueprintEntry.getValue()) {
                    String[] split = blueprintText.trim().split("=");
                    if (blueprintUtils.isBlueprintNamePreConfigured(blueprintText, split)) {
                        LOGGER.debug("Load default validation '{}'.", AnonymizerUtil.anonymize(blueprintText));
                        BlueprintV4Request blueprintJson = new BlueprintV4Request();
                        blueprintJson.setName(split[0].trim());
                        JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(
                                blueprintUtils.readDefaultBlueprintFromFile(blueprintEntry.getKey(), split));
                        JsonNode blueprintNode = jsonNode.get("blueprint");
                        blueprintJson.setBlueprint(blueprintNode.toString());
                        Blueprint bp = converter.convert(blueprintJson);
                        bp.setDefaultBlueprintText(bp.getBlueprintText());
                        JsonNode tags = jsonNode.get("tags");
                        Map<String, Object> tagParameters = blueprintUtils.prepareTags(tags);
                        bp.setTags(new Json(tagParameters));
                        JsonNode description = jsonNode.get("description");
                        bp.setDescription(description == null ? split[0] : description.asText(split[0]));
                        BlueprintFile bpf = new BlueprintFile.Builder()
                                .name(bp.getName())
                                .blueprintText(bp.getBlueprintText())
                                .defaultBlueprintText(bp.getDefaultBlueprintText())
                                .stackName(bp.getStackName())
                                .stackVersion(bp.getStackVersion())
                                .stackType(bp.getStackType())
                                .blueprintUpgradeOption(bp.getBlueprintUpgradeOption())
                                .hybridOption(bp.getHybridOption())
                                .hostGroupCount(bp.getHostGroupCount())
                                .description(bp.getDescription())
                                .tags(bp.getTags())
                                .build();
                        String fileName = split[1];

                        if (govCloudDeployment) {
                            if (govCloudExculsionFilter.shouldAddBlueprint(bp.getStackVersion(), fileName)) {
                                defaultBlueprints.put(bp.getName(), bpf);
                            }
                        } else {
                            defaultBlueprints.put(bp.getName(), bpf);
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Can not read default validation from file: ", e);
            }
        }
    }

    public Map<String, BlueprintFile> defaultBlueprints() {
        return defaultBlueprints;
    }

    private Map<String, Set<String>> blueprints() {
        return blueprintEntities.getDefaults()
                .entrySet()
                .stream()
                .filter(e -> StringUtils.isNoneBlank(e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> Sets.newHashSet(e.getValue().split(";"))));
    }
}
