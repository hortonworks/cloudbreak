package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
@Transactional
public class BlueprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintService.class);

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private ClusterRepository clusterRepository;

    public Set<Blueprint> retrievePrivateBlueprints(CbUser user) {
        return blueprintRepository.findForUser(user.getUserId());
    }

    public Set<Blueprint> retrieveAccountBlueprints(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return blueprintRepository.findAllInAccount(user.getAccount());
        } else {
            return blueprintRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Blueprint get(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        return blueprint;
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public Blueprint getByName(String name, CbUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInAccount(name, user.getAccount(), user.getUsername());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        return blueprint;
    }

    @Transactional(Transactional.TxType.NEVER)
    public Blueprint create(CbUser user, Blueprint blueprint, List<Map<String, Map<String, String>>> properties) {
        LOGGER.debug("Creating blueprint: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Blueprint savedBlueprint;
        blueprint.setOwner(user.getUserId());
        blueprint.setAccount(user.getAccount());
        if (properties != null && properties.size() != 0) {
            LOGGER.info("Extend blueprint with the following properties: {}", properties);
            Map<String, Map<String, String>> configs = new HashMap<>();
            for (Map<String, Map<String, String>> property : properties) {
                for (String key : property.keySet()) {
                    Map<String, String> configValues = configs.get(key);
                    if (configValues != null) {
                        configValues.putAll(property.get(key));
                    } else {
                        configs.put(key, property.get(key));
                    }
                }
            }
            String extendedBlueprint = new AmbariClient().extendBlueprintGlobalConfiguration(blueprint.getBlueprintText(), configs);
            LOGGER.info("Extended blueprint result: {}", extendedBlueprint);
            blueprint.setBlueprintText(extendedBlueprint);
        }
        try {
            savedBlueprint = blueprintRepository.save(blueprint);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.BLUEPRINT, blueprint.getName(), ex);
        }
        return savedBlueprint;
    }

    public void delete(Long id, CbUser user) {
        Blueprint blueprint = blueprintRepository.findByIdInAccount(id, user.getAccount());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        delete(blueprint, user);
    }

    public Blueprint getPublicBlueprint(String name, CbUser user) {
        Blueprint blueprint = blueprintRepository.findOneByName(name, user.getAccount());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        return blueprint;
    }

    public Blueprint getPrivateBlueprint(String name, CbUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInUser(name, user.getUserId());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        return blueprint;
    }

    public void delete(String name, CbUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        delete(blueprint, user);
    }

    @Transactional(Transactional.TxType.NEVER)
    public Iterable<Blueprint> save(Iterable<Blueprint> entities) {
        return blueprintRepository.save(entities);
    }

    private void delete(Blueprint blueprint, CbUser user) {
        if (clusterRepository.findAllClustersByBlueprint(blueprint.getId()).isEmpty()) {
            if (!user.getUserId().equals(blueprint.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
                throw new BadRequestException("Blueprints can only be deleted by account admins or owners.");
            }
            if (ResourceStatus.USER_MANAGED.equals(blueprint.getStatus())) {
                blueprintRepository.delete(blueprint);
            } else {
                blueprint.setStatus(ResourceStatus.DEFAULT_DELETED);
                blueprintRepository.save(blueprint);
            }
        } else {
            throw new BadRequestException(String.format(
                    "There are clusters associated with blueprint '%s'. Please remove these before deleting the blueprint.", blueprint.getId()));
        }
    }
}
