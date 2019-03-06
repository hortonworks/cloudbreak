package com.sequenceiq.cloudbreak.init.clusterdefinition;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.requests.ClusterDefinitionV4Request;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.ClusterTemplateUtils;
import com.sequenceiq.cloudbreak.converter.v4.clusterdefinition.ClusterDefinitionV4RequestToClusterDefinitionConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Service
public class DefaulClusterDefinitionCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaulClusterDefinitionCache.class);

    private final Map<String, ClusterDefinition> defaultBlueprints = new HashMap<>();

    @Value("#{'${cb.clusterdefinition.ambari.defaults:}'.split(';')}")
    private List<String> releasedBlueprints;

    @Value("#{'${cb.clusterdefinition.ambari.internal:}'.split(';')}")
    private List<String> internalBlueprints;

    @Value("#{'${cb.clusterdefinition.cm.defaults:}'.split(';')}")
    private List<String> releasedCMClusterDefinitions;

    @Inject
    private ClusterTemplateUtils clusterTemplateUtils;

    @Inject
    private ClusterDefinitionV4RequestToClusterDefinitionConverter converter;

    @PostConstruct
    public void loadBlueprintsFromFile() {
        List<String> blueprints = blueprints();
        for (String blueprintText : blueprints) {
            try {
                String[] split = blueprintText.trim().split("=");
                if (clusterTemplateUtils.isBlueprintNamePreConfigured(blueprintText, split)) {
                    LOGGER.debug("Load default validation '{}'.", blueprintText);
                    ClusterDefinitionV4Request blueprintJson = new ClusterDefinitionV4Request();
                    blueprintJson.setName(split[0].trim());
                    JsonNode jsonNode = clusterTemplateUtils.convertStringToJsonNode(clusterTemplateUtils.readDefaultBlueprintFromFile(split));
                    blueprintJson.setClusterDefinition(jsonNode.get("blueprint").toString());
                    ClusterDefinition bp = converter.convert(blueprintJson);
                    JsonNode tags = jsonNode.get("tags");
                    Map<String, Object> tagParameters = clusterTemplateUtils.prepareTags(tags);
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

    public Map<String, ClusterDefinition> defaultBlueprints() {
        Map<String, ClusterDefinition> result = new HashMap<>();
        defaultBlueprints.forEach((key, value) -> result.put(key, SerializationUtils.clone(value)));
        return result;
    }

    private List<String> blueprints() {
        return Stream.concat(
                Stream.concat(releasedBlueprints.stream(), internalBlueprints.stream()), releasedCMClusterDefinitions.stream()
        ).filter(StringUtils::isNoneBlank).collect(Collectors.toList());
    }
}
