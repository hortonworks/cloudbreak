package com.sequenceiq.cloudbreak.init.blueprint;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Service
public class DefaultBlueprintCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintCache.class);

    private final Map<String, Blueprint> defaultBlueprints = new HashMap<>();

    @Value("#{'${cb.blueprint.ambari.defaults:}'.split(';')}")
    private List<String> releasedBlueprints;

    @Value("#{'${cb.blueprint.ambari.internal:}'.split(';')}")
    private List<String> internalBlueprints;

    @Value("#{'${cb.blueprint.cm.defaults:}'.split(';')}")
    private List<String> releasedCMBlueprints;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private BlueprintV4RequestToBlueprintConverter converter;

    @PostConstruct
    public void loadBlueprintsFromFile() {
        List<String> blueprints = blueprints();
        for (String blueprintText : blueprints) {
            try {
                String[] split = blueprintText.trim().split("=");
                if (blueprintUtils.isBlueprintNamePreConfigured(blueprintText, split)) {
                    LOGGER.debug("Load default validation '{}'.", blueprintText);
                    BlueprintV4Request blueprintJson = new BlueprintV4Request();
                    blueprintJson.setName(split[0].trim());
                    JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(blueprintUtils.readDefaultBlueprintFromFile(split));
                    blueprintJson.setBlueprint(jsonNode.get("blueprint").toString());
                    Blueprint bp = converter.convert(blueprintJson);
                    JsonNode tags = jsonNode.get("tags");
                    Map<String, Object> tagParameters = blueprintUtils.prepareTags(tags);
                    bp.setTags(new Json(tagParameters));
                    JsonNode description = jsonNode.get("description");
                    bp.setDescription(description == null ? split[0] : description.asText(split[0]));
                    defaultBlueprints.put(bp.getName(), bp);
                }
            } catch (IOException e) {
                LOGGER.error("Can not read default validation from file: ", e);
            }
        }
    }

    public Map<String, Blueprint> defaultBlueprints() {
        Map<String, Blueprint> result = new HashMap<>();
        defaultBlueprints.forEach((key, value) -> result.put(key, SerializationUtils.clone(value)));
        return result;
    }

    private List<String> blueprints() {
        return Stream.concat(
                Stream.concat(releasedBlueprints.stream(), internalBlueprints.stream()), releasedCMBlueprints.stream()
        ).filter(StringUtils::isNoneBlank).collect(Collectors.toList());
    }
}
