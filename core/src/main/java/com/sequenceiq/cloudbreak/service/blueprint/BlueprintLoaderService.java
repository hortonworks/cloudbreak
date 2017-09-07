package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.common.type.ResourceStatus.DEFAULT_DELETED;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintInputParameters;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
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

    public Set<Blueprint> loadBlueprints(IdentityUser user) {
        Set<Blueprint> blueprints = new HashSet<>();
        List<String> blueprintNames = getDefaultBlueprintNames(user);
        for (String blueprintStrings : blueprintArray) {
            String[] split = blueprintStrings.split("=");
            if (blueprintUtils.isBlueprintNamePreConfigured(blueprintStrings, split) && !blueprintNames.contains(blueprintStrings)) {
                LOGGER.info("Adding default blueprint '{}' for user '{}'", blueprintStrings, user.getUsername());
                try {
                    BlueprintRequest blueprintJson = new BlueprintRequest();
                    blueprintJson.setName(split[0].trim());
                    blueprintJson.setDescription(split[0]);
                    JsonNode jsonNode = blueprintUtils.convertStringToJsonNode(blueprintUtils.readDefaultBlueprintFromFile(split));
                    blueprintJson.setAmbariBlueprint(blueprintUtils.convertStringToJsonNode(jsonNode.get("blueprint").toString()));
                    Blueprint bp = conversionService.convert(blueprintJson, Blueprint.class);
                    JsonNode inputs = jsonNode.get("inputs");
                    BlueprintInputParameters inputParameters = new BlueprintInputParameters(blueprintUtils.prepareInputs(inputs));
                    bp.setInputParameters(new Json(inputParameters));
                    bp.setOwner(user.getUserId());
                    bp.setAccount(user.getAccount());
                    bp.setPublicInAccount(true);
                    bp.setStatus(ResourceStatus.DEFAULT);
                    blueprintRepository.save(bp);
                    blueprints.add(bp);
                } catch (ConstraintViolationException | DataIntegrityViolationException e) {
                    LOGGER.info("Blueprint already added with name: '" + split[0] + "' for user: '" + user.getUsername() + "' user.");
                } catch (Exception e) {
                    LOGGER.error("Blueprint is not available for '" + user.getUsername() + "' user.", e);
                }
            }
        }
        return blueprints;
    }

    private List<String> getDefaultBlueprintNames(IdentityUser user) {
        return blueprintRepository.findAllDefaultInAccount(user.getAccount()).stream()
                .map(bp -> bp.getStatus() == DEFAULT_DELETED ? NameUtil.cutTimestampPostfix(bp.getName()) : bp.getName())
                .map(x -> toString().trim())
                .collect(Collectors.toList());
    }
}
