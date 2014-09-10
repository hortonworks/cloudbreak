package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
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
        return blueprintRepository.findForUser(user.getUsername());
    }

    @Override
    public Set<Blueprint> retrieveAccountBlueprints(CbUser user) {
        Set<Blueprint> blueprints = new HashSet<>();
        if (user.getRoles().contains("admin")) {
            blueprints = blueprintRepository.findAllInAccount(user.getAccount());
        } else {
            blueprints = blueprintRepository.findPublicsInAccount(user.getAccount());
        }
        return blueprints;
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
        blueprint.setOwner(user.getUsername());
        blueprint.setAccount(user.getAccount());
        Blueprint savedBlueprint = blueprintRepository.save(blueprint);
        websocketService.sendToTopicUser(user.getUsername(), WebsocketEndPoint.BLUEPRINT,
                new StatusMessage(savedBlueprint.getId(), savedBlueprint.getName(), Status.AVAILABLE.name()));
        return savedBlueprint;
    }

    @Override
    public void delete(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
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
}
