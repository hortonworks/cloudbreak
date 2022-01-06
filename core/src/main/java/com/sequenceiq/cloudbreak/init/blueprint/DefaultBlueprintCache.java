package com.sequenceiq.cloudbreak.init.blueprint;

import static com.sequenceiq.cloudbreak.util.FileReaderUtils.readFileFromClasspath;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.cloudera.cdp.shaded.com.google.common.collect.Sets;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.domain.BlueprintUpgradeOption;

@Component
@Scope("prototype")
public class DefaultBlueprintCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintCache.class);

    private final Map<String, BlueprintFile> defaultBlueprints = new HashMap<>();

    private final Map<String, NavigableSet<Versioned>> availableBlueprints = new HashMap<>();

    private String defaultBlueprintsDir = "defaults/blueprints";

    @Inject
    private BlueprintEntities blueprintEntities;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private BlueprintV4RequestToBlueprintConverter converter;

    @PostConstruct
    public void loadBlueprintsFromFile() {
        List<String> files;
        try {
            files = getFiles();
        } catch (Exception e) {
            LOGGER.warn("Failed to load files from: {}, original msg: {}", defaultBlueprintsDir, e.getMessage(), e);
            return;
        }
        if (!files.isEmpty()) {
            LOGGER.debug("Default blueprints is loaded into cache by resource dir: {}", String.join(", ", files));
            files.stream().filter(StringUtils::isNotBlank).forEach(
                    file -> {
                        try {
                            String templateAsString = readFileFromClasspath(file);
                            JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(templateAsString);
                            if (jsonNode == null) {
                                return;
                            }
                            BlueprintV4Request blueprintJson = new BlueprintV4Request();
                            JsonNode blueprintNode = jsonNode.get("blueprint");
                            if (blueprintNode == null) {
                                return;
                            }
                            blueprintJson.setBlueprint(blueprintNode.toString());
                            JsonNode descriptionNode = jsonNode.get("description");
                            if (descriptionNode == null) {
                                return;
                            }
                            blueprintJson.setName(descriptionNode.toString());
                            Blueprint bp = converter.convert(blueprintJson);
                            JsonNode tags = jsonNode.get("tags");
                            Map<String, Object> tagParameters = blueprintUtils.prepareTags(tags);
                            bp.setTags(new Json(tagParameters));
                            bp.setDescription(descriptionNode.asText());
                            JsonNode blueprintUpgradeOption = blueprintNode.isMissingNode() ? null : blueprintNode.get("blueprintUpgradeOption");
                            bp.setBlueprintUpgradeOption(getBlueprintUpgradeOption(blueprintUpgradeOption));
                            BlueprintFile bpf = new BlueprintFile.Builder()
                                    .name(bp.getName())
                                    .blueprintText(bp.getBlueprintText())
                                    .stackName(bp.getStackName())
                                    .stackVersion(bp.getStackVersion())
                                    .stackType(bp.getStackType())
                                    .blueprintUpgradeOption(bp.getBlueprintUpgradeOption())
                                    .hostGroupCount(bp.getHostGroupCount())
                                    .description(bp.getDescription())
                                    .build();
                            defaultBlueprints.put(bp.getName(), bpf);
                            String[] splits = file.split("/");
                            String fileName = splits[splits.length - 1];
                            String version = bp.getStackVersion();
                            availableBlueprints.compute(fileName, (k, v) -> (v == null) ? new TreeSet<>(new VersionComparator()) : v).add(() -> version);
                        } catch (IOException e) {
                            LOGGER.error("Can not read default validation from file: ", e);
                        }
                    }
            );
        } else {
            LOGGER.debug("No default blueprints");
            return;
        }
        Map<String, Set<String>> blueprints = blueprints();
        for (Map.Entry<String, Set<String>> blueprintEntry : blueprints.entrySet()) {
            for (String blueprintText : blueprintEntry.getValue()) {
                String[] split = blueprintText.trim().split("=");
                if (split.length < 2) {
                    return;
                }
                String fileName = split[1] + ".bp";
                String version = blueprintEntry.getKey();
                NavigableSet<Versioned> bpVersions = availableBlueprints.get(fileName);
                if (bpVersions == null) {
                    return;
                }
                Versioned floor = bpVersions.floor(() -> version);
                if (floor != null && !version.equals(floor.getVersion())) {
                    String bpName = split[0];
                    BlueprintFile oldBpFile = defaultBlueprints.get(bpName.replace(version, floor.getVersion()));
                    if (oldBpFile == null) {
                        return;
                    }
                    String newBpText = oldBpFile.getBlueprintText()
                            .replaceFirst("\"description\" *: *\"" + floor.getVersion(), "\"description\": \"" + version)
                            .replaceFirst("\"cdhVersion\" *: *\"" + floor.getVersion(), "\"cdhVersion\": \"" + version);
                    BlueprintFile bpf = new BlueprintFile.Builder()
                            .name(bpName)
                            .blueprintText(newBpText)
                            .stackName(oldBpFile.getStackName())
                            .stackVersion(version)
                            .stackType(oldBpFile.getStackType())
                            .blueprintUpgradeOption(oldBpFile.getBlueprintUpgradeOption())
                            .hostGroupCount(oldBpFile.getHostGroupCount())
                            .description(oldBpFile.getDescription().replace(floor.getVersion(), version))
                            .build();
                    defaultBlueprints.put(bpName, bpf);
                }
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

    private List<String> getFiles() throws IOException {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        return Arrays.stream(patternResolver.getResources("classpath:" + defaultBlueprintsDir + "/**/*.bp"))
                .map(resource -> {
                    try {
                        String[] path = resource.getURL().getPath().split(defaultBlueprintsDir);
                        return String.format("%s%s", defaultBlueprintsDir, path[1]);
                    } catch (IOException e) {
                        // wrap to runtime exception because of lambda and log the error in the caller method.
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toList());
    }

    private BlueprintUpgradeOption getBlueprintUpgradeOption(JsonNode blueprintUpgradeOption) {
        return Optional.ofNullable(blueprintUpgradeOption)
                .map(JsonNode::asText)
                .map(BlueprintUpgradeOption::valueOf)
                .orElse(BlueprintUpgradeOption.ENABLED);
    }
}
