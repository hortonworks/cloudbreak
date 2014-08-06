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
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.WebsocketEndPoint;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.UserRepository;
import com.sequenceiq.cloudbreak.service.company.CompanyService;
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
    private BlueprintConverter blueprintConverter;

    @Autowired
    private WebsocketService websocketService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyService companyService;

    public IdJson addBlueprint(User user, BlueprintJson blueprintJson) {
        Blueprint blueprint = blueprintConverter.convert(blueprintJson);
        blueprint.setUser(user);
        if (blueprint.getUserRoles().isEmpty()) {
            blueprint.getUserRoles().addAll(user.getUserRoles());
        }
        blueprint = blueprintRepository.save(blueprint);
        websocketService.sendToTopicUser(user.getEmail(), WebsocketEndPoint.BLUEPRINT,
                new StatusMessage(blueprint.getId(), blueprint.getName(), Status.CREATE_COMPLETED.name()));
        return new IdJson(blueprint.getId());
    }

    @Override
    public Set<Blueprint> getAll(User user) {
        Set<Blueprint> userBluePrints = user.getBlueprints();
        Set<Blueprint> legacyBlueprints = new HashSet<>();
        LOGGER.debug("User blueprints: #{}", userBluePrints.size());

        if (user.getUserRoles().contains(UserRole.COMPANY_ADMIN)) {
            LOGGER.debug("Getting company user blueprints for company admin; id: [{}]", user.getId());
            legacyBlueprints = getCompanyUserBlueprints(user);
        } else {
            LOGGER.debug("Getting company wide blueprints for company user; id: [{}]", user.getId());
            legacyBlueprints = getCompanyBlueprints(user);
        }
        LOGGER.debug("Found #{} legacy blueprints for user [{}]", legacyBlueprints.size(), user.getId());
        userBluePrints.addAll(legacyBlueprints);

        return userBluePrints;
    }

    private Set<Blueprint> getCompanyBlueprints(User user) {
        Set<Blueprint> companyBlueprints = new HashSet<>();
        User adminWithFilteredData = companyService.companyUserData(user.getCompany().getId(), user.getUserRoles().iterator().next());
        if (adminWithFilteredData != null) {
            companyBlueprints = adminWithFilteredData.getBlueprints();
        } else {
            LOGGER.debug("There's no company admin for user: [{}]", user.getId());
        }
        return companyBlueprints;
    }

    private Set<Blueprint> getCompanyUserBlueprints(User user) {
        Set<Blueprint> companyUserBlueprints = new HashSet<>();
        Set<User> companyUsers = companyService.companyUsers(user.getCompany().getId());
        companyUsers.remove(user);
        for (User cUser : companyUsers) {
            LOGGER.debug("Adding blueprints of company user: [{}]", cUser.getId());
            companyUserBlueprints.addAll(cUser.getBlueprints());
        }
        return companyUserBlueprints;
    }

    public Set<BlueprintJson> getAllForAdmin(User user) {
        Set<BlueprintJson> blueprints = new HashSet<>();
        Set<User> decoratedUsers = companyService.companyUsers(user.getCompany().getId());
        for (User cUser : decoratedUsers) {
            blueprints.addAll(blueprintConverter.convertAllEntityToJson(cUser.getBlueprints()));
        }
        return blueprints;
    }

    @Override
    public BlueprintJson get(Long id) {
        Blueprint blueprint = blueprintRepository.findOne(id);
        if (blueprint == null) {
            throw new NotFoundException(String.format("Blueprint '%s' not found.", id));
        }
        return blueprintConverter.convert(blueprint);
    }

    @Override
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
