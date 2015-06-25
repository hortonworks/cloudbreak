package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.EnvironmentVariableConfig.CB_BLUEPRINT_DEFAULTS;

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

import com.sequenceiq.cloudbreak.controller.json.BlueprintRequest;
import com.sequenceiq.cloudbreak.controller.json.JsonHelper;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.ResourceStatus;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Component
public class DefaultBlueprintLoaderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintLoaderService.class);

    @Value("#{'${cb.blueprint.defaults:" + CB_BLUEPRINT_DEFAULTS + "}'.split(',')}")
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
        if (blueprintRepository.findAllDefaultInAccount(user.getAccount()).isEmpty()) {
            blueprints.addAll(createDefaultBlueprints(user));
        }
        return blueprints;
    }

    private Set<Blueprint> createDefaultBlueprints(CbUser user) {
        Set<Blueprint> blueprints = new HashSet<>();
        for (String blueprintName : blueprintArray) {
            Blueprint oneByName = null;
            try {
                oneByName = blueprintRepository.findOneByName(blueprintName, user.getAccount());
            } catch (Exception e) {
                oneByName = null;
            }
            if (oneByName == null) {
                LOGGER.info("Adding default blueprint '{}' for user '{}'", blueprintName, user.getUsername());
                try {
                    BlueprintRequest blueprintJson = new BlueprintRequest();
                    blueprintJson.setName(blueprintName);
                    blueprintJson.setDescription(blueprintName);
                    blueprintJson.setAmbariBlueprint(jsonHelper.createJsonFromString(
                            FileReaderUtils.readFileFromClasspath(String.format("defaults/blueprints/%s.bp", blueprintName))));
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

}
