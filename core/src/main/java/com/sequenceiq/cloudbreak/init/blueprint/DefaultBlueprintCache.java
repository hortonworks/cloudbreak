package com.sequenceiq.cloudbreak.init.blueprint;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.requests.BlueprintV4Request;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintV4RequestToBlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintFile;
import com.sequenceiq.cloudbreak.init.blueprint.overlay.RuntimeBlueprintOverlayLoader;
import com.sequenceiq.cloudbreak.init.blueprint.overlay.RuntimeBlueprintOverlayLoader.MaterializedBlueprint;
import com.sequenceiq.cloudbreak.service.blueprint.CrnGeneratorService;

@Component
@Scope("prototype")
public class DefaultBlueprintCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintCache.class);

    private final Map<String, BlueprintFile> defaultBlueprints = new HashMap<>();

    @Value("${cb.runtimes.overlay.enabled:false}")
    private boolean runtimeOverlayEnabled;

    @Value("#{'${cb.runtimes.patched:}'.split(',')}")
    private List<String> patchedRuntimes = List.of();

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

    @Inject
    private CrnGeneratorService crnGeneratorService;

    @Inject
    private RuntimeBlueprintOverlayLoader runtimeBlueprintOverlayLoader;

    @PostConstruct
    public void loadBlueprintsFromFile() {
        Map<String, Set<String>> blueprints = blueprints();
        boolean govCloudDeployment = commonGovService.govCloudDeployment(
                preferencesService.enabledGovPlatforms(),
                preferencesService.enabledPlatforms());
        Set<String> generatedCrns = new HashSet<>();
        for (Map.Entry<String, Set<String>> blueprintEntry : blueprints.entrySet()) {
            try {
                for (String blueprintText : blueprintEntry.getValue()) {
                    String[] split = blueprintText.trim().split("=");
                    if (blueprintUtils.isBlueprintNamePreConfigured(blueprintText, split)) {
                        LOGGER.debug("Load default validation '{}'.", AnonymizerUtil.anonymize(blueprintText));
                        JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(
                                blueprintUtils.readDefaultBlueprintFromFile(blueprintEntry.getKey(), split));
                        registerBlueprint(split[0].trim(), jsonNode, split[1], govCloudDeployment, generatedCrns);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Can not read default validation from file: ", e);
            }
        }
        loadOverlayBlueprints(govCloudDeployment, generatedCrns);
    }

    /**
     * Reconstructs the blueprints for the patched runtime versions ({@code cb.runtimes.patched}) that ship as
     * sparse overlays on top of the frozen base version, and merges them into the cache. A real on-disk
     * blueprint of the same name always wins: overlays are skipped when the name is already present, so this
     * must run <em>after</em> the disk scan above. Overlay names never collide with the base's in practice
     * (they carry a different version prefix), so this is a safety net rather than a live conflict.
     */
    private void loadOverlayBlueprints(boolean govCloudDeployment, Set<String> generatedCrns) {
        if (!runtimeOverlayEnabled) {
            return;
        }
        Set<String> patchedVersions = patchedRuntimes.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .collect(Collectors.toSet());
        if (patchedVersions.isEmpty()) {
            return;
        }
        List<MaterializedBlueprint> overlays = runtimeBlueprintOverlayLoader.materializeOverlayBlueprints(patchedVersions);
        for (MaterializedBlueprint overlay : overlays) {
            if (defaultBlueprints.containsKey(overlay.name())) {
                LOGGER.debug("An on-disk blueprint already provides [{}]; keeping it over the overlay.", overlay.name());
                continue;
            }
            try {
                registerBlueprint(overlay.name(), overlay.fileJson(), overlay.fileStem(), govCloudDeployment, generatedCrns);
            } catch (JsonProcessingException e) {
                LOGGER.error("Can not register overlay blueprint {}: ", overlay.name(), e);
            }
        }
    }

    /**
     * Builds a {@link BlueprintFile} from a parsed {@code .bp} document and registers it under the converted
     * blueprint's name, applying the same CRN-uniqueness check and gov-cloud exclusion filter to every source
     * (on-disk base and materialized overlay alike). {@code name} is the configured/synthesized source name fed
     * to the converter; {@code fileStem} is the exclusion filter's key.
     */
    private void registerBlueprint(String name, JsonNode jsonNode, String fileStem, boolean govCloudDeployment, Set<String> generatedCrns)
            throws JsonProcessingException {
        BlueprintFile bpf = buildBlueprintFile(name, jsonNode, generatedCrns);
        if (govCloudDeployment) {
            if (govCloudExculsionFilter.shouldAddBlueprint(bpf.getStackVersion(), fileStem)) {
                defaultBlueprints.put(bpf.getName(), bpf);
            }
        } else {
            defaultBlueprints.put(bpf.getName(), bpf);
        }
    }

    private BlueprintFile buildBlueprintFile(String name, JsonNode jsonNode, Set<String> generatedCrns) throws JsonProcessingException {
        BlueprintV4Request blueprintJson = new BlueprintV4Request();
        blueprintJson.setName(name);
        JsonNode blueprintNode = jsonNode.get("blueprint");
        blueprintJson.setBlueprint(blueprintNode.toString());
        Blueprint bp = converter.convert(blueprintJson);
        bp.setDefaultBlueprintText(bp.getBlueprintText());
        JsonNode tags = jsonNode.get("tags");
        Map<String, Object> tagParameters = blueprintUtils.prepareTags(tags);
        bp.setTags(new Json(tagParameters));
        JsonNode description = jsonNode.get("description");
        bp.setDescription(description == null ? name : description.asText(name));
        String resourceCrn = crnGeneratorService.createGlobalDefaultBlueprintCrn(bp.getName());
        if (generatedCrns.contains(resourceCrn)) {
            throw new RuntimeException(String.format(
                    "%s global default blueprint crn was already generated from another blueprint name.", resourceCrn));
        }
        generatedCrns.add(resourceCrn);
        return new BlueprintFile.Builder()
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
                .resourceCrn(resourceCrn)
                .build();
    }

    public boolean isDefaultByName(String blueprintName) {
        return defaultBlueprints.containsKey(blueprintName);
    }

    public boolean isDefaultByCrn(String crn) {
        return defaultBlueprints.values()
                .stream()
                .anyMatch(b -> b.getResourceCrn().equals(crn));
    }

    public Optional<BlueprintFile> getDefaultByName(String blueprintName) {
        return Optional.ofNullable(defaultBlueprints.get(blueprintName));
    }

    public BlueprintFile getDefaultByCrn(String crn) {
        return defaultBlueprints.values()
                .stream()
                .filter(b -> b.getResourceCrn().equals(crn))
                .findFirst().orElseThrow(NotFoundException.notFound("Cluster template", crn));
    }

    public Set<String> getBlueprintVersions() {
        return defaultBlueprints.entrySet()
                .stream()
                .map(e -> e.getValue().getStackVersion())
                .collect(Collectors.toSet());
    }

    public Map<String, BlueprintFile> defaultBlueprints() {
        return defaultBlueprints;
    }

    protected void setPatchedRuntimes(List<String> patchedRuntimes) {
        this.patchedRuntimes = patchedRuntimes;
    }

    protected void setRuntimeOverlayEnabled(boolean runtimeOverlayEnabled) {
        this.runtimeOverlayEnabled = runtimeOverlayEnabled;
    }

    private Map<String, Set<String>> blueprints() {
        return blueprintEntities.getDefaults()
                .entrySet()
                .stream()
                .filter(e -> StringUtils.isNoneBlank(e.getValue()))
                .collect(Collectors.toMap(e -> e.getKey(), e -> Sets.newHashSet(e.getValue().split(";"))));
    }
}
