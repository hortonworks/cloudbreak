package com.sequenceiq.cloudbreak.service.blueprint;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.BlueprintParameter;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class BlueprintLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintLoaderService.class);

    @Value("#{'${cb.blueprint.defaults:}'.split(';')}")
    private List<String> blueprintArray;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private BlueprintUtils blueprintUtils;

    @PostConstruct
    public void updateDefaultBlueprints() {
        Iterable<Blueprint> allBlueprint = blueprintRepository.findAll();
        for (String blueprintStrings : blueprintArray) {
            String[] split = blueprintStrings.split("=");
            if (!blueprintStrings.isEmpty() && (split.length == 2 || split.length == 1) && !split[0].isEmpty()) {
                try {
                    String bpDefaultText = blueprintUtils.readDefaultBlueprintFromFile(split);
                    LOGGER.info("Updating default blueprint with name '{}'.", split[0]);
                    for (Blueprint blueprint : allBlueprint) {
                        if (blueprint.getName().equals(split[0]) && blueprint.getStatus().equals(ResourceStatus.DEFAULT)) {
                            LOGGER.info("Blueprint {} is a default blueprint with name '{}'. Updating with the new config.",
                                    blueprint.getId(), blueprint.getName());
                            JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(bpDefaultText);
                            JsonNode blueprintText = jsonNode.get("blueprint");
                            JsonNode inputs = jsonNode.get("inputs");
                            BlueprintInputParameters inputParameters = new BlueprintInputParameters(prepareInputs(inputs));
                            blueprint.setInputParameters(new Json(inputParameters));
                            blueprint.setBlueprintText(blueprintText.toString());
                            blueprint.setHostGroupCount(blueprintUtils.countHostGroups(blueprintText));
                            blueprint.setBlueprintName(blueprintUtils.getBlueprintName(blueprintText));
                            blueprintRepository.save(blueprint);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.info("Updating default blueprint with name '{}' wasn't successful because default blueprint file couldn't read: {}.", split[0], e);
                }
            }
        }
    }

    private List<BlueprintParameter> prepareInputs(JsonNode inputs) throws com.fasterxml.jackson.core.JsonProcessingException {
        Set<BlueprintParameter> blueprintParameters = new HashSet<>();
        if (inputs.isArray()) {
            for (final JsonNode objNode : inputs) {
                BlueprintParameter blueprintParameter = JsonUtil.treeToValue(objNode, BlueprintParameter.class);
                blueprintParameters.add(blueprintParameter);
            }
        }
        return blueprintParameters.stream().collect(Collectors.toList());
    }

    public Set<Blueprint> loadBlueprints(CbUser user) {
        Set<Blueprint> blueprints = new HashSet<>();
        Set<String> blueprintNames = getDefaultBlueprintNames(user);
        for (String blueprintStrings : blueprintArray) {
            String[] split = blueprintStrings.split("=");
            if (!blueprintStrings.isEmpty() && (split.length == 2 || split.length == 1) && !blueprintNames.contains(blueprintStrings)
                    && !split[0].isEmpty()) {
                LOGGER.info("Adding default blueprint '{}' for user '{}'", blueprintStrings, user.getUsername());
                try {
                    BlueprintRequest blueprintJson = new BlueprintRequest();
                    blueprintJson.setName(split[0]);
                    blueprintJson.setDescription(split[0]);
                    JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(blueprintUtils.readDefaultBlueprintFromFile(split));
                    blueprintJson.setAmbariBlueprint(blueprintUtils.convertStringToJsonNode(jsonNode.get("blueprint").toString()));
                    Blueprint bp = conversionService.convert(blueprintJson, Blueprint.class);
                    JsonNode inputs = jsonNode.get("inputs");
                    BlueprintInputParameters inputParameters = new BlueprintInputParameters(prepareInputs(inputs));
                    bp.setInputParameters(new Json(inputParameters));
                    bp.setOwner(user.getUserId());
                    bp.setAccount(user.getAccount());
                    bp.setPublicInAccount(true);
                    bp.setStatus(ResourceStatus.DEFAULT);
                    blueprintRepository.save(bp);
                    blueprints.add(bp);
                } catch (Exception e) {
                    LOGGER.error("Blueprint is not available for '{}' user.", e, user);
                }
            }
        }
        return blueprints;
    }

    private Set<String> getDefaultBlueprintNames(CbUser user) {
        Set<String> defaultBpNames = new HashSet<>();
        Set<Blueprint> defaultBlueprints = blueprintRepository.findAllDefaultInAccount(user.getAccount());
        for (Blueprint defaultBlueprint : defaultBlueprints) {
            defaultBpNames.add(defaultBlueprint.getName());
        }
        return defaultBpNames;
    }

}
