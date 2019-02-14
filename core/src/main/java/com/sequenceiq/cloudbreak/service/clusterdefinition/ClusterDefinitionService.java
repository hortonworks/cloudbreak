package com.sequenceiq.cloudbreak.service.clusterdefinition;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.PRE_DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.controller.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.clusterdefinition.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.clusterdefinition.utils.AmbariBlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterDefinitionView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.init.clusterdefinition.ClusterDefinitionLoaderService;
import com.sequenceiq.cloudbreak.repository.ClusterDefinitionRepository;
import com.sequenceiq.cloudbreak.repository.ClusterDefinitionViewRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class ClusterDefinitionService extends AbstractWorkspaceAwareResourceService<ClusterDefinition> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterDefinitionService.class);

    private static final Set<Status> DELETED_CLUSTER_STATUSES
            = Set.of(PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS, DELETE_FAILED, DELETE_COMPLETED);

    private static final String SHARED_SERVICES_READY = "shared_services_ready";

    @Inject
    private ClusterDefinitionRepository clusterDefinitionRepository;

    @Inject
    private ClusterDefinitionViewRepository clusterDefinitionViewRepository;

    @Inject
    private AmbariBlueprintUtils ambariBlueprintUtils;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    @Inject
    private ClusterDefinitionLoaderService clusterDefinitionLoaderService;

    public ClusterDefinition get(Long id) {
        return clusterDefinitionRepository.findById(id).orElseThrow(notFound("Blueprint", id));
    }

    public ClusterDefinition create(Workspace workspace, ClusterDefinition clusterDefinition, Collection<Map<String, Map<String, String>>> properties,
            User user) {
        LOGGER.debug("Creating blueprint: Workspace: {} ({})", workspace.getId(), workspace.getName());
        ClusterDefinition savedClusterDefinition;
        if (properties != null && !properties.isEmpty()) {
            LOGGER.debug("Extend blueprint with the following properties: {}", properties);
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
            String clusterDefinitionText = clusterDefinition.getClusterDefinitionText();
            String extendedAmbariBlueprint = ambariBlueprintProcessorFactory.get(clusterDefinitionText)
                    .extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(configs), false).asText();
            LOGGER.debug("Extended blueprint result: {}", extendedAmbariBlueprint);
            clusterDefinition.setClusterDefinitionText(extendedAmbariBlueprint);
        }
        try {
            savedClusterDefinition = create(clusterDefinition, workspace.getId(), user);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.BLUEPRINT, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedClusterDefinition;
    }

    @Override
    public Set<ClusterDefinition> findAllByWorkspace(Workspace workspace) {
        return getAllAvailableInWorkspace(workspace);
    }

    @Override
    public Set<ClusterDefinition> findAllByWorkspaceId(Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        return getAllAvailableInWorkspace(workspace);
    }

    public Set<ClusterDefinitionView> getAllAvailableViewInWorkspace(Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        Set<ClusterDefinition> clusterDefinitions = clusterDefinitionRepository.findAllByWorkspaceIdAndStatus(workspace.getId(), ResourceStatus.DEFAULT);
        if (clusterDefinitionLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(clusterDefinitions)) {
            LOGGER.debug("Modifying blueprints based on the defaults for the '{}' workspace.", workspace.getId());
            clusterDefinitionLoaderService.loadBlueprintsForTheWorkspace(clusterDefinitions, workspace, this::saveDefaultsWithReadRight);
            LOGGER.debug("Blueprint modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        }
        return clusterDefinitionViewRepository.findAllByNotDeletedInWorkspace(workspace.getId());
    }

    public Set<ClusterDefinition> getAllAvailableInWorkspace(Workspace workspace) {
        Set<ClusterDefinition> clusterDefinitions = clusterDefinitionRepository.findAllByNotDeletedInWorkspace(workspace.getId());
        if (clusterDefinitionLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(clusterDefinitions)) {
            LOGGER.debug("Modifying blueprints based on the defaults for the '{}' workspace.", workspace.getId());
            clusterDefinitions = clusterDefinitionLoaderService.loadBlueprintsForTheWorkspace(clusterDefinitions, workspace, this::saveDefaultsWithReadRight);
            LOGGER.debug("Blueprint modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        }
        return clusterDefinitions;
    }

    public boolean isDatalakeBlueprint(ClusterDefinition clusterDefinition) {
        return Optional.ofNullable((Boolean) clusterDefinition.getTags().getMap().get(SHARED_SERVICES_READY)).orElse(false);
    }

    private Iterable<ClusterDefinition> saveDefaultsWithReadRight(Iterable<ClusterDefinition> blueprints, Workspace workspace) {
        blueprints.forEach(bp -> bp.setWorkspace(workspace));
        return clusterDefinitionRepository.saveAll(blueprints);
    }

    public ClusterDefinition delete(Long id) {
        return delete(get(id));
    }

    public boolean isClouderaManagerBlueprint(ClusterDefinition clusterDefinition) {
        return !isAmbariBlueprint(clusterDefinition);
    }

    public boolean isAmbariBlueprint(ClusterDefinition clusterDefinition) {
        return ambariBlueprintUtils.isAmbariBlueprint(clusterDefinition.getClusterDefinitionText());
    }

    @Override
    public ClusterDefinition delete(ClusterDefinition clusterDefinition) {
        LOGGER.debug("Deleting blueprint with name: {}", clusterDefinition.getName());
        prepareDeletion(clusterDefinition);
        if (ResourceStatus.USER_MANAGED.equals(clusterDefinition.getStatus())) {
            clusterDefinitionRepository.delete(clusterDefinition);
        } else {
            clusterDefinition.setName(NameUtil.postfixWithTimestamp(clusterDefinition.getName()));
            clusterDefinition.setStatus(ResourceStatus.DEFAULT_DELETED);
            clusterDefinition = clusterDefinitionRepository.save(clusterDefinition);
        }
        return clusterDefinition;
    }

    @Override
    public WorkspaceResourceRepository<ClusterDefinition, Long> repository() {
        return clusterDefinitionRepository;
    }

    @Override
    public WorkspaceResource resource() {
        return WorkspaceResource.CLUSTER_DEFINITION;
    }

    @Override
    protected void prepareDeletion(ClusterDefinition clusterDefinition) {
        Set<Cluster> notDeletedClustersWithThisBp = getNotDeletedClustersWithBlueprint(clusterDefinition);
        if (!notDeletedClustersWithThisBp.isEmpty()) {
            if (notDeletedClustersWithThisBp.size() > 1) {
                String clusters = notDeletedClustersWithThisBp
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with blueprint '%s'. Please remove these before deleting the blueprint. "
                                + "The following clusters are using this blueprint: [%s]", clusterDefinition.getName(), clusters));
            }
            throw new BadRequestException(String.format("There is a cluster ['%s'] which uses blueprint '%s'. Please remove this "
                    + "cluster before deleting the blueprint", notDeletedClustersWithThisBp.iterator().next().getName(), clusterDefinition.getName()));
        }
    }

    private Set<Cluster> getNotDeletedClustersWithBlueprint(ClusterDefinition clusterDefinition) {
        Set<Cluster> clustersWithThisBlueprint = clusterService.findByBlueprint(clusterDefinition);
        Set<Cluster> deletedClustersWithThisBp = clustersWithThisBlueprint.stream()
                .filter(cluster -> DELETED_CLUSTER_STATUSES.contains(cluster.getStatus()))
                .collect(Collectors.toSet());
        deletedClustersWithThisBp.forEach(cluster -> cluster.setClusterDefinition(null));
        clusterService.saveAll(deletedClustersWithThisBp);
        Set<Cluster> notDeletedClustersWithThisBp = new HashSet<>(clustersWithThisBlueprint);
        notDeletedClustersWithThisBp.removeAll(deletedClustersWithThisBp);
        return notDeletedClustersWithThisBp;
    }

    @Override
    protected void prepareCreation(ClusterDefinition resource) {
    }

    private Set<String> queryCustomParameters(String name, Long workspaceId) {
        ClusterDefinition blueprint = getByNameForWorkspaceId(name, workspaceId);
        String blueprintText = blueprint.getClusterDefinitionText();
        return centralBlueprintParameterQueryService.queryCustomParameters(blueprintText);
    }

    public Map<String, String> queryCustomParametersMap(String name, Long workspaceId) {
        Set<String> customParameters = queryCustomParameters(name, workspaceId);
        Map<String, String> result = new HashMap<>();
        for (String customParameter : customParameters) {
            result.put(customParameter, "");
        }
        return result;
    }

    public Set<ConfigQueryEntry> queryFileSystemParameters(String blueprintName, String clusterName,
            String storageName, String fileSystemType, String accountName, boolean attachedCluster, Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        ClusterDefinition clusterDefinition = getByNameForWorkspace(blueprintName, workspace);
        String clusterDefinitionText = clusterDefinition.getClusterDefinitionText();
        // Not necessarily the best way to figure out whether a DL or not. At the moment, DLs cannot be launched as
        // workloads, so works fine. Would be better to get this information from the invoking context itself.
        boolean datalake = ambariBlueprintUtils.isSharedServiceReadyBlueprint(clusterDefinition);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withClusterName(clusterName)
                .withStorageName(storageName)
                .withBlueprintText(clusterDefinitionText)
                .withFileSystemType(fileSystemType)
                .withAccountName(accountName)
                .withAttachedCluster(attachedCluster)
                .withDatalakeCluster(datalake)
                .build();

        return centralBlueprintParameterQueryService.queryFileSystemParameters(fileSystemConfigQueryObject);
    }
}
