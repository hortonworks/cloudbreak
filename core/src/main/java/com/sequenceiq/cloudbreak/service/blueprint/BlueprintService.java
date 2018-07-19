package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.blueprint.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.blueprint.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.blueprint.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class BlueprintService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintService.class);

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    @Inject
    private UserService userService;

    @Inject
    private OrganizationService organizationService;

    public Set<Blueprint> retrievePrivateBlueprints(IdentityUser user) {
        return blueprintRepository.findForUser(user.getUserId());
    }

    public Set<Blueprint> retrieveAccountBlueprints(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? blueprintRepository.findAllInAccount(user.getAccount())
                : blueprintRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
    }

    public Blueprint get(Long id) {
        return blueprintRepository.findById(id).orElseThrow(notFound("Blueprint", id));
    }

    public Blueprint getByName(String name, IdentityUser user) {
        return blueprintRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
    }

    public Blueprint get(String name, String account) {
        return blueprintRepository.findOneByName(name, account);
    }

    public Blueprint create(IdentityUser identityUser, Blueprint blueprint, Collection<Map<String, Map<String, String>>> properties) {
        LOGGER.debug("Creating blueprint: [User: '{}', Account: '{}']", identityUser.getUsername(), identityUser.getAccount());
        Blueprint savedBlueprint;
        blueprint.setOwner(identityUser.getUserId());
        blueprint.setAccount(identityUser.getAccount());
        User user = userService.getOrCreate(identityUser);
        Organization organization = organizationService.getDefaultOrganizationForUser(user);
        blueprint.setOrganization(organization);
        if (properties != null && !properties.isEmpty()) {
            LOGGER.info("Extend blueprint with the following properties: {}", properties);
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
            LOGGER.info("Extended blueprint result: {}", extendedBlueprint);
            blueprint.setBlueprintText(extendedBlueprint);
        }
        try {
            savedBlueprint = blueprintRepository.save(blueprint);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.BLUEPRINT, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedBlueprint;
    }

    public void delete(Long id, IdentityUser user) {
        Blueprint blueprint = blueprintRepository.findByIdInAccount(id, user.getAccount());
        delete(blueprint);
    }

    public Blueprint getPublicBlueprint(String name, IdentityUser user) {
        return blueprintRepository.findOneByName(name, user.getAccount());
    }

    public Blueprint getPrivateBlueprint(String name, IdentityUser user) {
        return blueprintRepository.findByNameInUser(name, user.getUserId());
    }

    public void delete(String name, IdentityUser user) {
        Blueprint blueprint = blueprintRepository.findByNameInAccount(name, user.getAccount(), user.getUserId());
        delete(blueprint);
    }

    public Iterable<Blueprint> save(Iterable<Blueprint> entities) {
        return blueprintRepository.saveAll(entities);
    }

    public void delete(Blueprint blueprint) {
        LOGGER.info("Deleting blueprint with name: {}", blueprint.getName());
        List<Cluster> clustersWithThisBlueprint = clusterService.getByBlueprint(blueprint);
        if (!clustersWithThisBlueprint.isEmpty()) {
            if (clustersWithThisBlueprint.size() > 1) {
                String clusters = clustersWithThisBlueprint
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with blueprint '%s'. Please remove these before deleting the blueprint. "
                                + "The following clusters are using this blueprint: [%s]", blueprint.getName(), clusters));
            } else {
                throw new BadRequestException(String.format("There is a cluster ['%s'] which uses blueprint '%s'. Please remove this "
                        + "cluster before deleting the blueprint", clustersWithThisBlueprint.get(0).getName(), blueprint.getName()));
            }
        }
        if (ResourceStatus.USER_MANAGED.equals(blueprint.getStatus())) {
            blueprintRepository.delete(blueprint);
        } else {
            blueprint.setName(NameUtil.postfixWithTimestamp(blueprint.getName()));
            blueprint.setStatus(ResourceStatus.DEFAULT_DELETED);
            blueprintRepository.save(blueprint);
        }
    }

    public Set<String> queryCustomParameters(String name, IdentityUser user) {
        Blueprint blueprint = getPublicBlueprint(name, user);
        return centralBlueprintParameterQueryService.queryCustomParameters(blueprint.getBlueprintText());
    }

    public Set<ConfigQueryEntry> queryFileSystemParameters(String blueprintName, String clusterName,
            String storageName, String fileSystemType, String accountName, boolean attachedCluster, IdentityUser user) {
        Blueprint blueprint = getPublicBlueprint(blueprintName, user);

        FileSystemConfigQueryObject fileSystemConfigQueryObject = FileSystemConfigQueryObject.Builder.builder()
                .withClusterName(clusterName)
                .withStorageName(storageName)
                .withBlueprintText(blueprint.getBlueprintText())
                .withFileSystemType(fileSystemType)
                .withAccountName(accountName)
                .withAttachedCluster(attachedCluster)
                .build();

        return centralBlueprintParameterQueryService.queryFileSystemParameters(fileSystemConfigQueryObject);
    }
}
