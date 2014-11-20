package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

@Service
public class DefaultBlueprintService implements BlueprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBlueprintService.class);

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private WebsocketService websocketService;

    @Override
    public Set<Blueprint> retrievePrivateBlueprints(CbUser user) {
        return blueprintRepository.findForUser(user.getUserId());
    }

    @Override
    public Set<Blueprint> retrieveAccountBlueprints(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return blueprintRepository.findAllInAccount(user.getAccount());
        } else {
            return blueprintRepository.findPublicsInAccount(user.getAccount());
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
    public Blueprint get(String name, CbUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInAccount(name, user.getAccount());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        return blueprint;
    }

    @Override
    public Blueprint create(CbUser user, Blueprint blueprint) {
        MDCBuilder.buildMdcContext(blueprint);
        LOGGER.debug("Creating blueprint: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        Blueprint savedBlueprint = null;
        blueprint.setOwner(user.getUserId());
        blueprint.setAccount(user.getAccount());
        try {
            savedBlueprint = blueprintRepository.save(blueprint);
            websocketService.sendToTopicUser(user.getUsername(), WebsocketEndPoint.BLUEPRINT,
                    new StatusMessage(savedBlueprint.getId(), savedBlueprint.getName(), Status.AVAILABLE.name()));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(blueprint.getName(), ex);
        }
        return savedBlueprint;
    }

    @Override
    public void delete(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
        MDCBuilder.buildMdcContext(blueprint);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        if (clusterRepository.findAllClusterByBlueprint(blueprint.getId()).isEmpty()) {

            blueprintRepository.delete(blueprint);
            websocketService.sendToTopicUser(blueprint.getOwner(), WebsocketEndPoint.BLUEPRINT,
                    new StatusMessage(blueprint.getId(), blueprint.getName(), Status.DELETE_COMPLETED.name()));
        } else {
            throw new BadRequestException(String.format(
                    "There are stacks associated with blueprint '%s'. Please remove these before the deleting the blueprint.", id));
        }
    }

    @Override
    public void delete(String name, CbUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInAccount(name, user.getAccount());
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", name));
        }
        MDCBuilder.buildMdcContext(blueprint);

        if (clusterRepository.findAllClusterByBlueprint(blueprint.getId()).isEmpty()) {

            blueprintRepository.delete(blueprint);
            websocketService.sendToTopicUser(blueprint.getOwner(), WebsocketEndPoint.BLUEPRINT,
                    new StatusMessage(blueprint.getId(), blueprint.getName(), Status.DELETE_COMPLETED.name()));
        } else {
            throw new BadRequestException(String.format(
                    "There are stacks associated with blueprint '%s'. Please remove these before the deleting the blueprint.", name));
        }
    }
}
