package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.model.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.model.Status.PRE_DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.BlueprintViewRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class BlueprintService extends AbstractWorkspaceAwareResourceService<Blueprint> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintService.class);

    private static final Set<Status> DELETED_CLUSTER_STATUSES
            = Set.of(PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS, DELETE_FAILED, DELETE_COMPLETED);

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private BlueprintViewRepository blueprintViewRepository;

    @Inject
    private ClusterService clusterService;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    @Inject
    private BlueprintLoaderService blueprintLoaderService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    public Blueprint get(Long id) {
        return blueprintRepository.findById(id).orElseThrow(notFound("Blueprint", id));
    }

    public Blueprint create(Workspace workspace, Blueprint blueprint, Collection<Map<String, Map<String, String>>> properties, User user) {
        LOGGER.debug("Creating blueprint: Workspace: {} ({})", workspace.getId(), workspace.getName());
        Blueprint savedBlueprint;
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
            String blueprintText = blueprint.getBlueprintText();
            String extendedBlueprint = blueprintProcessorFactory.get(blueprintText)
                    .extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(configs), false).asText();
            LOGGER.info("Extended blueprint result: {}", extendedBlueprint);
            blueprint.setBlueprintText(extendedBlueprint);
        }
        try {
            savedBlueprint = create(blueprint, workspace.getId(), user);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.BLUEPRINT, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedBlueprint;
    }

    @Override
    public Set<Blueprint> findAllByWorkspace(Workspace workspace) {
        return getAllAvailableInWorkspace(workspace);
    }

    @Override
    public Set<Blueprint> findAllByWorkspaceId(Long workspaceId) {
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        User user = userService.getOrCreate(cloudbreakUser);
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        return getAllAvailableInWorkspace(workspace);
    }

    public Set<BlueprintView> getAllAvailableViewInWorkspace(Workspace workspace) {
        Set<Blueprint> blueprints = blueprintRepository.findAllByWorkspaceIdAndStatus(workspace.getId(), ResourceStatus.DEFAULT);
        if (blueprintLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(blueprints)) {
            LOGGER.info("Modifying blueprints based on the defaults for the '{}' workspace.", workspace.getId());
            blueprintLoaderService.loadBlueprintsForTheWorkspace(blueprints, workspace, this::saveDefaultsWithReadRight);
            LOGGER.info("Blueprint modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        }
        return blueprintViewRepository.findAllByNotDeletedInWorkspace(workspace.getId());
    }

    public Set<Blueprint> getAllAvailableInWorkspace(Workspace workspace) {
        Set<Blueprint> blueprints = blueprintRepository.findAllByNotDeletedInWorkspace(workspace.getId());
        if (blueprintLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(blueprints)) {
            LOGGER.info("Modifying blueprints based on the defaults for the '{}' workspace.", workspace.getId());
            blueprints = blueprintLoaderService.loadBlueprintsForTheWorkspace(blueprints, workspace, this::saveDefaultsWithReadRight);
            LOGGER.info("Blueprint modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        }
        return blueprints;
    }

    private Iterable<Blueprint> saveDefaultsWithReadRight(Iterable<Blueprint> blueprints, Workspace workspace) {
        blueprints.forEach(bp -> bp.setWorkspace(workspace));
        return blueprintRepository.saveAll(blueprints);
    }

    public Blueprint delete(Long id) {
        return delete(get(id));
    }

    @Override
    public Blueprint delete(Blueprint blueprint) {
        LOGGER.info("Deleting blueprint with name: {}", blueprint.getName());
        prepareDeletion(blueprint);
        if (ResourceStatus.USER_MANAGED.equals(blueprint.getStatus())) {
            blueprintRepository.delete(blueprint);
        } else {
            blueprint.setName(NameUtil.postfixWithTimestamp(blueprint.getName()));
            blueprint.setStatus(ResourceStatus.DEFAULT_DELETED);
            blueprint = blueprintRepository.save(blueprint);
        }
        return blueprint;
    }

    @Override
    public WorkspaceResourceRepository<Blueprint, Long> repository() {
        return blueprintRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.BLUEPRINT;
    }

    @Override
    protected void prepareDeletion(Blueprint blueprint) {
        Set<Cluster> notDeletedClustersWithThisBp = getNotDeletedClustersWithBlueprint(blueprint);
        if (!notDeletedClustersWithThisBp.isEmpty()) {
            if (notDeletedClustersWithThisBp.size() > 1) {
                String clusters = notDeletedClustersWithThisBp
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with blueprint '%s'. Please remove these before deleting the blueprint. "
                                + "The following clusters are using this blueprint: [%s]", blueprint.getName(), clusters));
            }
            throw new BadRequestException(String.format("There is a cluster ['%s'] which uses blueprint '%s'. Please remove this "
                    + "cluster before deleting the blueprint", notDeletedClustersWithThisBp.iterator().next().getName(), blueprint.getName()));
        }
    }

    private Set<Cluster> getNotDeletedClustersWithBlueprint(Blueprint blueprint) {
        Set<Cluster> clustersWithThisBlueprint = clusterService.findByBlueprint(blueprint);
        Set<Cluster> deletedClustersWithThisBp = clustersWithThisBlueprint.stream()
                .filter(cluster -> DELETED_CLUSTER_STATUSES.contains(cluster.getStatus()))
                .collect(Collectors.toSet());
        deletedClustersWithThisBp.forEach(cluster -> cluster.setBlueprint(null));
        clusterService.saveAll(deletedClustersWithThisBp);
        Set<Cluster> notDeletedClustersWithThisBp = new HashSet<>(clustersWithThisBlueprint);
        notDeletedClustersWithThisBp.removeAll(deletedClustersWithThisBp);
        return notDeletedClustersWithThisBp;
    }

    @Override
    protected void prepareCreation(Blueprint resource) {
    }

    public Set<String> queryCustomParameters(String name, Workspace workspace) {
        Blueprint blueprint = getByNameForWorkspace(name, workspace);
        String blueprintText = blueprint.getBlueprintText();
        return centralBlueprintParameterQueryService.queryCustomParameters(blueprintText);
    }

    public Set<ConfigQueryEntry> queryFileSystemParameters(String blueprintName, String clusterName,
            String storageName, String fileSystemType, String accountName, boolean attachedCluster, Workspace workspace) {
        Blueprint blueprint = getByNameForWorkspace(blueprintName, workspace);
        String blueprintText = blueprint.getBlueprintText();
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withClusterName(clusterName)
                .withStorageName(storageName)
                .withBlueprintText(blueprintText)
                .withFileSystemType(fileSystemType)
                .withAccountName(accountName)
                .withAttachedCluster(attachedCluster)
                .build();

        return centralBlueprintParameterQueryService.queryFileSystemParameters(fileSystemConfigQueryObject);
    }
}
