package com.sequenceiq.cloudbreak.init;

import java.io.FileNotFoundException;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintUtils;

@Component
public class BlueprintUpdateService implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintUpdateService.class);

    @Value("#{'${cb.blueprint.defaults:}'.split(';')}")
    private List<String> blueprintArray;

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Iterable<Blueprint> allBlueprint = blueprintRepository.findAll();
        for (String blueprintStrings : blueprintArray) {
            String[] split = blueprintStrings.split("=");
            if (blueprintUtils.isBlueprintNamePreConfigured(blueprintStrings, split)) {
                try {
                    String bpDefaultText = blueprintUtils.readDefaultBlueprintFromFile(split);
                    LOGGER.info("Updating default blueprint with name '{}'.", split[0]);
                    for (Blueprint blueprint : allBlueprint) {
                        if (blueprint.getName().trim().equals(split[0].trim()) && blueprint.getStatus().equals(ResourceStatus.DEFAULT)) {
                            readAndUpdateBlueprint(bpDefaultText, blueprint);
                        }
                    }
                } catch (FileNotFoundException e) {
                    LOGGER.error("Failed to update blueprint because file not found on path: {}.", split[0]);
                } catch (Exception e) {
                    LOGGER.error("Updating default blueprint with name '{}' wasn't successful because error occurred under the update process: {}.",
                            split[0], e);
                }
            }
        }
    }

    private void readAndUpdateBlueprint(String bpDefaultText, Blueprint blueprint) throws Exception {
        LOGGER.info("Blueprint {} is a default blueprint with name '{}'. Updating with the new config.", blueprint.getId(), blueprint.getName());
        JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(bpDefaultText);
        JsonNode blueprintText = jsonNode.get("blueprint");
        JsonNode inputs = jsonNode.get("inputs");
        BlueprintInputParameters inputParameters = new BlueprintInputParameters(blueprintUtils.prepareInputs(inputs));
        blueprint.setInputParameters(new Json(inputParameters));
        blueprint.setBlueprintText(blueprintText.toString());
        blueprint.setHostGroupCount(blueprintUtils.countHostGroups(blueprintText));
        blueprint.setBlueprintName(blueprintUtils.getBlueprintName(blueprintText));
        blueprint.setName(blueprint.getName().trim());
        blueprintRepository.save(blueprint);
    }
}
