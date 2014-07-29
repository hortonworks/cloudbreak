package com.sequenceiq.cloudbreak.service.blueprint;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.json.BlueprintJson;
import com.sequenceiq.cloudbreak.controller.json.IdJson;
import com.sequenceiq.cloudbreak.converter.BlueprintConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Company;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.websocket.WebsocketService;
import com.sequenceiq.cloudbreak.websocket.message.StatusMessage;

@Service
public class BlueprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintService.class);

    @Autowired
    private BlueprintRepository blueprintRepository;

    @Autowired
    private ClusterRepository clusterRepository;

    @Autowired
    private BlueprintConverter blueprintConverter;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private UserRepository userRepository;

    public IdJson addBlueprint(User user, BlueprintJson blueprintJson) {
        Blueprint blueprint = blueprintConverter.convert(blueprintJson);
        blueprint.setUser(user);
        blueprint = blueprintRepository.save(blueprint);
        websocketService.sendToTopicUser(user.getEmail(), WebsocketEndPoint.BLUEPRINT,
                new StatusMessage(blueprint.getId(), blueprint.getName(), Status.CREATE_COMPLETED.name()));
        return new IdJson(blueprint.getId());
    }

    public Set<BlueprintJson> getAll(User user) {
        return blueprintConverter.convertAllEntityToJson(user.getBlueprints());
    }

    public Set<BlueprintJson> getAllForAdmin(User user) {
        Set<BlueprintJson> blueprints = new HashSet<>();
        Company company = user.getCompany();
        User decoratedUser = null;
        for (User cUser : company.getUsers()) {
            decoratedUser = userRepository.findOneWithLists(cUser.getId());
            blueprints.addAll(blueprintConverter.convertAllEntityToJson(decoratedUser.getBlueprints()));
        }
        return blueprints;
    }


    public BlueprintJson get(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        return blueprintConverter.convert(blueprint);
    }

    public void delete(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        if (clusterRepository.findAllClusterByBlueprint(blueprint.getId()).isEmpty()) {

            blueprintRepository.delete(blueprint);
            websocketService.sendToTopicUser(blueprint.getUser().getEmail(), WebsocketEndPoint.BLUEPRINT,
                    new StatusMessage(blueprint.getId(), blueprint.getName(), Status.DELETE_COMPLETED.name()));
        } else {
            throw new BadRequestException(String.format(
                    "There are stacks associated with blueprint '%s'. Please remove these before the deleting the blueprint.", id));
        }

    }
}
