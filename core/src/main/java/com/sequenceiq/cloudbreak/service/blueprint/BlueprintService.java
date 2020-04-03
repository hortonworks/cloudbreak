package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT_DELETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.Crn.ResourceType;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cmtemplate.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.BlueprintViewRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.CloudResourceAdvisor;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject.Builder;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;
import com.sequenceiq.common.api.type.CdpResourceType;

@Service
public class BlueprintService extends AbstractWorkspaceAwareResourceService<Blueprint> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintService.class);

    private static final String MULTI_HOSTNAME_EXCEPTION_MESSAGE_FORMAT = "Host %s names must be unique! The following host %s names are invalid due to " +
            "their multiple occurrence: %s";

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private BlueprintViewRepository blueprintViewRepository;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private ClusterService clusterService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private CentralBlueprintParameterQueryService centralBlueprintParameterQueryService;

    @Inject
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

    @Inject
    private BlueprintLoaderService blueprintLoaderService;

    @Inject
    private CloudResourceAdvisor cloudResourceAdvisor;

    @Inject
    private BlueprintListFilters blueprintListFilters;

    public Blueprint get(Long id) {
        return blueprintRepository.findById(id).orElseThrow(notFound("Cluster definition", id));
    }

    public Blueprint createForLoggedInUser(Blueprint blueprint, Long workspaceId, String accountId, String creator) {
        if (getVersionForCmWithBlueprintProcessingExceptionHandling(blueprint.getBlueprintText()).isPresent()) {
            validateHostNames(cmTemplateProcessorFactory.get(blueprint.getBlueprintText()));
        } else {
            throw new BadRequestException("Invalid CM template!");
        }
        decorateWithCrn(blueprint, accountId, creator);
        return super.createForLoggedInUser(blueprint, workspaceId);
    }

    public Blueprint deleteByWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        return nameOrCrn.hasName()
                ? super.deleteByNameFromWorkspace(nameOrCrn.getName(), workspaceId)
                : delete(blueprintRepository.findByResourceCrnAndWorkspaceId(nameOrCrn.getCrn(), workspaceId)
                .orElseThrow(() -> notFound("blueprint", nameOrCrn.getCrn()).get()));
    }

    public Blueprint getByWorkspace(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        return nameOrCrn.hasName()
                ? getByNameForWorkspaceId(nameOrCrn.getName(), workspaceId)
                : getByCrnAndWorkspaceIdAndAddToMdc(nameOrCrn.getCrn(), workspaceId);
    }

    public void decorateWithCrn(Blueprint bp, String accountId, String creator) {
        bp.setResourceCrn(createCRN(accountId));
        bp.setCreator(creator);
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
            LOGGER.debug("Extended blueprint result: {}", AnonymizerUtil.anonymize(blueprintText));
            blueprint.setBlueprintText(blueprintText);
        }
        try {
            savedBlueprint = create(blueprint, workspace.getId(), user);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], %s", APIResourceType.BLUEPRINT, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg, ex);
        }
        return savedBlueprint;
    }

    public PlatformRecommendation getRecommendation(Long workspaceId, String blueprintName, String credentialName,
            String region, String platformVariant, String availabilityZone, CdpResourceType cdpResourceType) {
        if (!ObjectUtils.allNotNull(region)) {
            throw new BadRequestException("region cannot be null");
        }
        return cloudResourceAdvisor.createForBlueprint(workspaceId, blueprintName, credentialName, region, platformVariant, availabilityZone, cdpResourceType);
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

    public Set<BlueprintView> getAllAvailableViewInWorkspaceAndFilterBySdxReady(Long workspaceId, Boolean withSdx) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        updateDefaultBlueprintCollection(workspace);
        Set<BlueprintView> allByNotDeletedInWorkspace = blueprintViewRepository.findAllByNotDeletedInWorkspace(workspaceId);
        allByNotDeletedInWorkspace = allByNotDeletedInWorkspace.stream()
                .filter(b -> blueprintListFilters.isDistroXDisplayed(b)).collect(Collectors.toSet());
        if (withSdx) {
            return allByNotDeletedInWorkspace;
        }
        return allByNotDeletedInWorkspace.stream()
                .filter(it -> !blueprintListFilters.isDatalakeBlueprint(it)).collect(Collectors.toSet());
    }

    public Set<Blueprint> getAllAvailableInWorkspace(Workspace workspace) {
        updateDefaultBlueprintCollection(workspace);
        return blueprintRepository.findAllByNotDeletedInWorkspace(workspace.getId());
    }

    public Blueprint getByNameForWorkspaceAndLoadDefaultsIfNecessary(String name, Workspace workspace) {
        Set<Blueprint> blueprints = blueprintRepository.findAllByNotDeletedInWorkspace(workspace.getId());
        Optional<Blueprint> blueprint = filterBlueprintsByName(name, blueprints);
        if (blueprint.isPresent()) {
            return blueprint.get();
        } else {
            Set<Blueprint> updatedDefaultBlueprints = updateDefaultBlueprintCollection(workspace);
            blueprint = filterBlueprintsByName(name, updatedDefaultBlueprints);
            if (blueprint.isPresent()) {
                return blueprint.get();
            }
        }
        throw new NotFoundException(String.format("No cluster template found with name '%s'", name));
    }

    public void updateDefaultBlueprintCollection(Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        updateDefaultBlueprintCollection(workspace);
    }

    private Set<Blueprint> updateDefaultBlueprintCollection(Workspace workspace) {
        Set<Blueprint> blueprintsInDatabase = blueprintRepository.findAllByWorkspaceIdAndStatusIn(workspace.getId(),
                Set.of(DEFAULT, DEFAULT_DELETED, USER_MANAGED));
        if (!blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(blueprintsInDatabase)) {
            return blueprintsInDatabase.stream()
                    .filter(bp -> DEFAULT.equals(bp.getStatus()) || DEFAULT_DELETED.equals(bp.getStatus()))
                    .collect(Collectors.toSet());
        }
        LOGGER.debug("Modifying blueprints based on the defaults for the '{}' workspace.", workspace.getId());
        Set<Blueprint> updatedBlueprints =
                blueprintLoaderService.loadBlueprintsForTheWorkspace(blueprintsInDatabase, workspace, this::saveDefaultsWithReadRight);
        LOGGER.debug("Blueprint modifications finished based on the defaults for '{}' workspace.", workspace.getId());
        return updatedBlueprints;
    }

    private Optional<Blueprint> filterBlueprintsByName(String name, Collection<Blueprint> blueprints) {
        return blueprints.stream().filter(blueprint -> name.equals(blueprint.getName())).findFirst();
    }

    public boolean isDatalakeBlueprint(Blueprint blueprint) {
        return blueprintListFilters.isDatalakeBlueprint(blueprint);
    }

    private Iterable<Blueprint> saveDefaultsWithReadRight(Iterable<Blueprint> blueprints, Workspace workspace) {
        blueprints.forEach(bp -> bp.setWorkspace(workspace));
        return blueprintRepository.saveAll(blueprints);
    }

    public boolean isClouderaManagerTemplate(Blueprint blueprint) {
        return blueprintUtils.isClouderaManagerClusterTemplate(blueprint.getBlueprintText());
    }

    public String getBlueprintVariant(Blueprint blueprint) {
        return blueprintUtils.getBlueprintVariant(blueprint.getBlueprintText());
    }

    @Override
    public Blueprint getByNameForWorkspace(String name, Workspace workspace) {
        return getByNameForWorkspaceAndLoadDefaultsIfNecessary(name, workspace);
    }

    @Override
    public Blueprint delete(Blueprint blueprint) {
        LOGGER.debug("Deleting blueprint with name: {}", blueprint.getName());
        prepareDeletion(blueprint);
        if (USER_MANAGED.equals(blueprint.getStatus())) {
            blueprintRepository.delete(blueprint);
        } else {
            LOGGER.error("Tried to delete DEFAULT blueprint");
            throw new BadRequestException("deletion of DEFAULT blueprint is not allowed");
        }
        return blueprint;
    }

    @Override
    public WorkspaceResourceRepository<Blueprint, Long> repository() {
        return blueprintRepository;
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
                        "There are clusters associated with cluster template '%s'. Please remove these before deleting the cluster template. "
                                + "The following clusters are using this blueprint: [%s]", blueprint.getName(), clusters));
            }
            throw new BadRequestException(String.format("There is a cluster ['%s'] which uses cluster template '%s'. Please remove this "
                    + "cluster before deleting the custer template", notDeletedClustersWithThisCd.iterator().next().getName(), blueprint.getName()));
        }
    }

    private Set<Cluster> getNotDeletedClustersWithBlueprint(Blueprint blueprint) {
        Set<Cluster> clustersWithThisBlueprint = clusterService.findByBlueprint(blueprint);
        Set<Cluster> deletedClustersWithThisBp = clustersWithThisBlueprint.stream()
                .filter(cluster -> {
                    Stack stack = cluster.getStack();
                    return DELETE_COMPLETED.equals(stack.getStatus());
                })
                .collect(Collectors.toSet());
        deletedClustersWithThisBp.forEach(cluster -> cluster.setBlueprint(null));
        clusterService.saveAll(deletedClustersWithThisBp);
        Set<Cluster> notDeletedClustersWithThisBp = new HashSet<>(clustersWithThisBlueprint);
        notDeletedClustersWithThisBp.removeAll(deletedClustersWithThisBp);
        return notDeletedClustersWithThisBp;
    }

    @Override
    protected void prepareCreation(Blueprint resource) {
        resource.setCreated(System.currentTimeMillis());
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
            String baseLocation, String fileSystemType, String accountName, boolean attachedCluster,
            boolean secure, Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        Blueprint blueprint = getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace);
        String blueprintText = blueprint.getBlueprintText();
        // Not necessarily the best way to figure out whether a DL or not. At the moment, DLs cannot be launched as
        // workloads, so works fine. Would be better to get this information from the invoking context itself.
        boolean datalake = blueprintUtils.isSharedServiceReadyBlueprint(blueprint);
        FileSystemConfigQueryObject fileSystemConfigQueryObject = Builder.builder()
                .withClusterName(clusterName)
                .withStorageName(StringUtils.stripEnd(baseLocation, "/"))
                .withBlueprintText(blueprintText)
                .withFileSystemType(fileSystemType)
                .withAccountName(accountName)
                .withAttachedCluster(attachedCluster)
                .withDatalakeCluster(datalake)
                .withSecure(secure)
                .build();

        Set<ConfigQueryEntry> result = new HashSet<>();

        if (blueprintUtils.isClouderaManagerClusterTemplate(blueprintText)) {
            result = cmCloudStorageConfigProvider.queryParameters(fileSystemConfigQueryObject);
        }
        return result;
    }

    private void validateHostNames(BlueprintTextProcessor blueprintTextProcessor) {
        LOGGER.debug("Validating CM template host names...");
        List<String> hostTemplateNames = blueprintTextProcessor.getHostTemplateNames();
        if (hostNamesAreNotUnique(hostTemplateNames)) {
            String nonUniqueHostTemplateNames = String.join(", ", findHostTemplateNameDuplicates(hostTemplateNames));
            String hostSectionIdentifier = blueprintTextProcessor.getHostGroupPropertyIdentifier();
            String message = String.format(MULTI_HOSTNAME_EXCEPTION_MESSAGE_FORMAT, hostSectionIdentifier, hostSectionIdentifier, nonUniqueHostTemplateNames);
            throw new BadRequestException(message);
        }
    }

    private Optional<String> getVersionForCmWithBlueprintProcessingExceptionHandling(String bpText) {
        try {
            return cmTemplateProcessorFactory.get(bpText).getVersion();
        } catch (BlueprintProcessingException ignore) {
            LOGGER.info("Unable to serialize blueprint text as a CM template!");
            return Optional.empty();
        }
    }

    private Set<String> findHostTemplateNameDuplicates(List<String> hostTemplateNames) {
        Set<String> temp = new LinkedHashSet<>();
        return hostTemplateNames.stream().filter(hostTemplateName -> !temp.add(hostTemplateName)).collect(Collectors.toSet());
    }

    private boolean hostNamesAreNotUnique(List<String> hostGroupNames) {
        return new HashSet<>(hostGroupNames).size() != hostGroupNames.size();
    }

    private Blueprint getByCrnAndWorkspaceIdAndAddToMdc(String crn, Long workspaceId) {
        Blueprint bp = blueprintRepository.findByResourceCrnAndWorkspaceId(crn, workspaceId)
                .orElseThrow(() -> notFound("cluster template", crn).get());
        MDCBuilder.buildMdcContext(bp);
        return bp;
    }

    private String createCRN(String accountId) {
        return Crn.builder()
                .setService(Crn.Service.DATAHUB)
                .setAccountId(accountId)
                .setResourceType(ResourceType.CLUSTER_DEFINITION)
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
    }
}
