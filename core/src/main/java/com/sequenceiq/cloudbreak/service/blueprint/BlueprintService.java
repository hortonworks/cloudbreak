package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ParametersQueryResponse;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.blueprint.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class BlueprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintService.class);

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    public Set<Blueprint> retrievePrivateBlueprints(IdentityUser user) {
        return blueprintRepository.findForUser(user.getUserId());
    }

    public Set<Blueprint> retrieveAccountBlueprints(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? blueprintRepository.findAllInAccount(user.getAccount())
                : blueprintRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    public Blueprint get(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        authorizationService.hasReadPermission(blueprint);
        return blueprint;
    }

    public Blueprint getByName(String name, IdentityUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        authorizationService.hasReadPermission(blueprint);
        return blueprint;
    }

    public Blueprint get(String name, String account) {
        Blueprint blueprint = blueprintRepository.findOneByName(name, account);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found in %s account.", name, account));
        }
        authorizationService.hasReadPermission(blueprint);
        return blueprint;
    }

    public Blueprint create(IdentityUser user, Blueprint blueprint, Collection<Map<String, Map<String, String>>> properties) {
        LOGGER.debug("Creating blueprint: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Blueprint savedBlueprint;
        blueprint.setOwner(user.getUserId());
        blueprint.setAccount(user.getAccount());
        if (properties != null && !properties.isEmpty()) {
            LOGGER.info("Extend validation with the following properties: {}", properties);
            Map<String, Map<String, String>> configs = new HashMap<>(properties.size());
            for (Map<String, Map<String, String>> property : properties) {
                for (Entry<String, Map<String, String>> entry : property.entrySet()) {
                    Map<String, String> configValues = configs.get(entry.getKey());
                    if (configValues != null) {
                        configValues.putAll(entry.getValue());
                    } else {
                        configs.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            String extendedBlueprint = blueprintProcessorFactory.get(blueprint.getBlueprintText())
                    .extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(configs), false).asText();
            LOGGER.info("Extended validation result: {}", extendedBlueprint);
            blueprint.setBlueprintText(extendedBlueprint);
        }
        try {
            savedBlueprint = blueprintRepository.save(blueprint);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.BLUEPRINT, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
        return savedBlueprint;
    }

    public void delete(Long id, IdentityUser user) {
        Blueprint blueprint = blueprintRepository.findByIdInAccount(id, user.getAccount());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        delete(blueprint);
    }

    public Blueprint getPublicBlueprint(String name, IdentityUser user) {
        Blueprint blueprint = blueprintRepository.findOneByName(name, user.getAccount());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        return blueprint;
    }

    public Blueprint getPrivateBlueprint(String name, IdentityUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInUser(name, user.getUserId());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        return blueprint;
    }

    public void delete(String name, IdentityUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        delete(blueprint);
    }

    public Iterable<Blueprint> save(Iterable<Blueprint> entities) {
        return blueprintRepository.save(entities);
    }

    private void delete(Blueprint blueprint) {
        authorizationService.hasWritePermission(blueprint);
        if (!clusterRepository.countByBlueprint(blueprint).equals(0L)) {
            throw new BadRequestException(String.format(
                    "There are clusters associated with validation '%s'. Please remove these before deleting the validation.", blueprint.getId()));
        }
        if (ResourceStatus.USER_MANAGED.equals(blueprint.getStatus())) {
            blueprintRepository.delete(blueprint);
        } else {
            blueprint.setName(NameUtil.postfixWithTimestamp(blueprint.getName()));
            blueprint.setStatus(ResourceStatus.DEFAULT_DELETED);
            blueprintRepository.save(blueprint);
        }
    }

    public ParametersQueryResponse queryCustomParameters(String name, IdentityUser user) {
        Blueprint blueprint = getPublicBlueprint(name, user);
        Map<String, String> result = new HashMap<>();
        for (String customParameter : centralBlueprintParameterQueryService.queryCustomParameters(blueprint.getBlueprintText())) {
            result.put(customParameter, "");
        }
        ParametersQueryResponse parametersQueryResponse = new ParametersQueryResponse();
        parametersQueryResponse.setCustom(result);
        return parametersQueryResponse;
    }
}
