package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.ResourceStatus;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
public class DefaultBlueprintService implements BlueprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintService.class);

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Override
    public Set<Blueprint> retrievePrivateBlueprints(CbUser user) {
        return blueprintRepository.findForUser(user.getUserId());
    }

    @Override
    public Set<Blueprint> retrieveAccountBlueprints(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return blueprintRepository.findAllInAccount(user.getAccount());
        } else {
            return blueprintRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    @Override
    public Blueprint get(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        return blueprint;
    }

    @Override
    public Blueprint create(CbUser user, Blueprint blueprint) {
        LOGGER.debug("Creating blueprint: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Blueprint savedBlueprint = null;
        blueprint.setOwner(user.getUserId());
        blueprint.setAccount(user.getAccount());
        try {
            savedBlueprint = blueprintRepository.save(blueprint);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.BLUEPRINT, blueprint.getName(), ex);
        }
        return savedBlueprint;
    }

    @Override
    public void delete(Long id, CbUser user) {
        Blueprint blueprint = blueprintRepository.findByIdInAccount(id, user.getAccount());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        delete(blueprint, user);
    }

    @Override
    public Blueprint getPublicBlueprint(String name, CbUser user) {
        Blueprint blueprint = blueprintRepository.findOneByName(name, user.getAccount());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        return blueprint;
    }

    @Override
    public Blueprint getPrivateBlueprint(String name, CbUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInUser(name, user.getUserId());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        return blueprint;
    }

    @Override
    public void delete(String name, CbUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        delete(blueprint, user);
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
