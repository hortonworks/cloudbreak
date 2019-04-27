package com.sequenceiq.cloudbreak.service.blueprint;

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
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.blueprint.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.BlueprintViewRepository;
import com.sequenceiq.cloudbreak.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject.Builder;
import com.sequenceiq.cloudbreak.template.filesystem.query.ConfigQueryEntry;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
public class BlueprintService extends AbstractWorkspaceAwareResourceService<Blueprint> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintService.class);

    private static final Set<Status> DELETED_CLUSTER_STATUSES
            = Set.of(PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS, DELETE_FAILED, DELETE_COMPLETED);

    private static final String SHARED_SERVICES_READY = "shared_services_ready";

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private BlueprintViewRepository blueprintViewRepository;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private ClusterService clusterService;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    @Inject
    private BlueprintLoaderService blueprintLoaderService;

    public Blueprint get(Long id) {
        return blueprintRepository.findById(id).orElseThrow(notFound("Cluster definition", id));
    }

    public Blueprint create(Workspace workspace, Blueprint blueprint, Collection<Map<String, Map<String, String>>> properties,
            User user) {
        LOGGER.debug("Creating blueprint: Workspace: {} ({})", workspace.getId(), workspace.getName());
        Blueprint savedBlueprint;
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
            String blueprintText = blueprint.getBlueprintText();
            String extendedAmbariBlueprint = ambariBlueprintProcessorFactory.get(blueprintText)
                    .extendBlueprintGlobalConfiguration(SiteConfigurations.fromMap(configs), false).asText();
            LOGGER.debug("Extended blueprint result: {}", extendedAmbariBlueprint);
            blueprint.setBlueprintText(extendedAmbariBlueprint);
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
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        return getAllAvailableInWorkspace(workspace);
    }

    public Set<BlueprintView> getAllAvailableViewInWorkspace(Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        Set<Blueprint> blueprints = blueprintRepository.findAllByWorkspaceIdAndStatus(workspace.getId(), ResourceStatus.DEFAULT);
        if (blueprintLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(blueprints)) {
            LOGGER.debug("Modifying blueprints based on the defaults for the '{}' workspace.", workspace.getId());
            blueprintLoaderService.loadClusterDEfinitionsForTheWorkspace(blueprints, workspace, this::saveDefaultsWithReadRight);
            LOGGER.debug("Cluster definition modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        }
        return blueprintViewRepository.findAllByNotDeletedInWorkspace(workspace.getId());
    }

    public Set<Blueprint> getAllAvailableInWorkspace(Workspace workspace) {
        Set<Blueprint> blueprints = blueprintRepository.findAllByNotDeletedInWorkspace(workspace.getId());
        if (blueprintLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(blueprints)) {
            LOGGER.debug("Modifying blueprints based on the defaults for the '{}' workspace.", workspace.getId());
            blueprints = blueprintLoaderService.loadClusterDEfinitionsForTheWorkspace(blueprints, workspace,
                    this::saveDefaultsWithReadRight);
            LOGGER.debug("Cluster definition modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        }
        return blueprints;
    }

    public Blueprint getByNameForWorkspaceAndLoadDefaultsIfNecessary(String name, Workspace workspace) {
        Set<Blueprint> blueprints = blueprintRepository.findAllByNotDeletedInWorkspace(workspace.getId());
        Optional<Blueprint> blueprint = filterBlueprintsByName(name, blueprints);
        if (blueprint.isPresent()) {
            return blueprint.get();
        } else {
            if (blueprintLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(blueprints)) {
                LOGGER.debug("Modifying blueprints based on the defaults for the '{}' workspace.", workspace.getId());
                blueprints = blueprintLoaderService.loadClusterDEfinitionsForTheWorkspace(blueprints, workspace,
                        this::saveDefaultsWithReadRight);
                LOGGER.debug("Cluster definition modifications finished based on the defaults for '{}' workspace.", workspace.getId());
                blueprint = filterBlueprintsByName(name, blueprints);
                if (blueprint.isPresent()) {
                    return blueprint.get();
                }
            }
        }
        throw new NotFoundException(String.format("No %s found with name '%s'", resource().getShortName(), name));
    }

    private Optional<Blueprint> filterBlueprintsByName(String name, Collection<Blueprint> blueprints) {
        return blueprints.stream().filter(blueprint -> name.equals(blueprint.getName())).findFirst();
    }

    public void updateDefaultBlueprints(Long workspaceId) {
        Set<Blueprint> blueprints = blueprintRepository.findAllByWorkspaceIdAndStatus(workspaceId, ResourceStatus.DEFAULT);
        if (blueprintLoaderService.addingDefaultBlueprintsAreNecessaryForTheUser(blueprints)) {
            LOGGER.debug("Modifying blueprints based on the defaults for the '{}' workspace.", workspaceId);
            blueprintLoaderService.loadClusterDEfinitionsForTheWorkspace(blueprints, getWorkspaceService().getByIdForCurrentUser(workspaceId),
                    this::saveDefaultsWithReadRight);
            LOGGER.debug("Cluster definition modifications finished based on the defaults for '{}' workspace.", workspaceId);
        }
    }

    public boolean isDatalakeBlueprint(Blueprint blueprint) {
        return Optional.ofNullable((Boolean) blueprint.getTags().getMap().get(SHARED_SERVICES_READY)).orElse(false);
    }

    private Iterable<Blueprint> saveDefaultsWithReadRight(Iterable<Blueprint> blueprints, Workspace workspace) {
        blueprints.forEach(bp -> bp.setWorkspace(workspace));
        return blueprintRepository.saveAll(blueprints);
    }

    public Blueprint delete(Long id) {
        return delete(get(id));
    }

    public boolean isClouderaManagerTemplate(Blueprint blueprint) {
        return blueprintUtils.isClouderaManagerClusterTemplate(blueprint.getBlueprintText());
    }

    public boolean isAmbariBlueprint(Blueprint blueprint) {
        return blueprintUtils.isAmbariBlueprint(blueprint.getBlueprintText());
    }

    public String getBlueprintVariant(Blueprint blueprint) {
        return isAmbariBlueprint(blueprint) ? ClusterApi.AMBARI : ClusterApi.CLOUDERA_MANAGER;
    }

    @Override
    public Blueprint delete(Blueprint blueprint) {
        LOGGER.debug("Deleting blueprint with name: {}", blueprint.getName());
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
        Set<Cluster> notDeletedClustersWithThisCd = getNotDeletedClustersWithBlueprint(blueprint);
        if (!notDeletedClustersWithThisCd.isEmpty()) {
            if (notDeletedClustersWithThisCd.size() > 1) {
                String clusters = notDeletedClustersWithThisCd
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with blueprint '%s'. Please remove these before deleting the blueprint. "
                                + "The following clusters are using this blueprint: [%s]", blueprint.getName(), clusters));
            }
            throw new BadRequestException(String.format("There is a cluster ['%s'] which uses blueprint '%s'. Please remove this "
                    + "cluster before deleting the blueprint", notDeletedClustersWithThisCd.iterator().next().getName(), blueprint.getName()));
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

    private Set<String> queryCustomParameters(String name, Long workspaceId) {
        Blueprint blueprint = getByNameForWorkspaceId(name, workspaceId);
        String blueprintText = blueprint.getBlueprintText();
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
        Blueprint blueprint = getByNameForWorkspace(blueprintName, workspace);
        String blueprintText = blueprint.getBlueprintText();
        // Not necessarily the best way to figure out whether a DL or not. At the moment, DLs cannot be launched as
        // workloads, so works fine. Would be better to get this information from the invoking context itself.
        boolean datalake = blueprintUtils.isSharedServiceReadyBlueprint(blueprint);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withClusterName(clusterName)
                .withStorageName(storageName)
                .withBlueprintText(blueprintText)
                .withFileSystemType(fileSystemType)
                .withAccountName(accountName)
                .withAttachedCluster(attachedCluster)
                .withDatalakeCluster(datalake)
                .build();

        return centralBlueprintParameterQueryService.queryFileSystemParameters(fileSystemConfigQueryObject);
    }
}
