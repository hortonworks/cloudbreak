package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

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
    private JsonHelper jsonHelper;

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
                    blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(
                            FileReaderUtils.readFileFromClasspath(String.format("defaults/blueprints/%s.bp", split.length == 2 ? split[1] : split[0]))));
                    Blueprint bp = conversionService.convert(blueprintJson, Blueprint.class);
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
