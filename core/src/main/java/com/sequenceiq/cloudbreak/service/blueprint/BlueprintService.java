package com.sequenceiq.cloudbreak.service.blueprint;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.DEFAULT_DELETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.SERVICE_MANAGED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus.USER_MANAGED;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.validation.FieldError;
import org.springframework.validation.MapBindingResult;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.CompositeAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.RecommendationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.aspect.Measure;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.model.AutoscaleRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.ScaleRecommendation;
import com.sequenceiq.cloudbreak.cmtemplate.CentralBlueprintParameterQueryService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hue.HueRoles;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.converter.v4.clustertemplate.PlatformRecommendationToPlatformRecommendationV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.projection.BlueprintStatusView;
import com.sequenceiq.cloudbreak.domain.view.BaseBlueprintClusterView;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.domain.view.CompactView;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.init.blueprint.BlueprintLoaderService;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.repository.BlueprintViewRepository;
import com.sequenceiq.cloudbreak.service.AbstractWorkspaceAwareResourceService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.CloudResourceAdvisor;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateViewService;
import com.sequenceiq.cloudbreak.template.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigQueryObject.Builder;
import com.sequenceiq.cloudbreak.validation.HueWorkaroundValidatorService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntry;
import com.sequenceiq.common.api.type.CdpResourceType;

@Service
public class BlueprintService extends AbstractWorkspaceAwareResourceService<Blueprint> implements CompositeAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlueprintService.class);

    @Inject
    private TransactionService transactionService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private BlueprintRepository blueprintRepository;

    @Inject
    private BlueprintViewRepository blueprintViewRepository;

    @Inject
    private BlueprintUtils blueprintUtils;

    @Inject
    private ClusterService clusterService;

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

    @Inject
    private BlueprintValidator blueprintValidator;

    @Inject
    private HueWorkaroundValidatorService hueWorkaroundValidatorService;

    @Inject
    private BlueprintConfigValidator blueprintConfigValidator;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private ClusterTemplateViewService clusterTemplateViewService;

    @Inject
    private PlatformRecommendationToPlatformRecommendationV4ResponseConverter platformRecommendationToPlatformRecommendationV4ResponseConverter;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    public Blueprint get(Long id) {
        return blueprintRepository.findById(id).orElseThrow(notFound("Cluster definition", id));
    }

    public Blueprint createForLoggedInUser(Blueprint blueprint, Long workspaceId, String accountId, String creator) {
        validate(blueprint, false);
        decorateWithCrn(blueprint, accountId);
        ownerAssignmentService.assignResourceOwnerRoleIfEntitled(creator, blueprint.getResourceCrn());
        try {
            return super.createForLoggedInUserInTransaction(blueprint, workspaceId);
        } catch (RuntimeException e) {
            ownerAssignmentService.notifyResourceDeleted(blueprint.getResourceCrn());
            throw e;
        }
    }

    public Blueprint createWithInternalUser(Blueprint blueprint, Long workspaceId, String accountId) {
        validate(blueprint, true);
        blueprint.setResourceCrn(createCRN(accountId));
        try {
            return transactionService.required(() -> {
                Workspace workspace = getWorkspaceService().getByIdWithoutAuth(workspaceId);
                blueprint.setWorkspace(workspace);
                blueprint.setStatus(ResourceStatus.SERVICE_MANAGED);
                return super.pureSave(blueprint);
            });
        } catch (TransactionService.TransactionExecutionException e) {
            throw new TransactionService.TransactionRuntimeExecutionException(e);
        }
    }

    private void validate(Blueprint blueprint, boolean internalCreation) {
        MapBindingResult errors = new MapBindingResult(new HashMap(), "blueprint");
        blueprintValidator.validate(blueprint, errors);
        if (errors.hasErrors()) {
            throw new BadRequestException(errors.getAllErrors().stream()
                    .map(e -> (e instanceof FieldError ? ((FieldError) e).getField() + ": " : "") + e.getDefaultMessage())
                    .collect(Collectors.joining("; ")));
        }
        if (!internalCreation) {
            blueprintConfigValidator.validate(blueprint);
        }
        hueWorkaroundValidatorService.validateForBlueprintRequest(getHueHostGroups(blueprint.getBlueprintJsonText()));
    }

    public Blueprint deleteByWorkspace(NameOrCrn nameOrCrn, Long workspaceId) {
        Blueprint deleted = nameOrCrn.hasName()
                ? super.deleteByNameFromWorkspace(nameOrCrn.getName(), workspaceId)
                : delete(blueprintRepository.findByResourceCrnAndWorkspaceId(nameOrCrn.getCrn(), workspaceId)
                .orElseThrow(() -> notFound("blueprint", nameOrCrn.getCrn()).get()));
        ownerAssignmentService.notifyResourceDeleted(deleted.getResourceCrn());
        return deleted;
    }

    public Blueprint getByWorkspace(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        return nameOrCrn.hasName()
                ? getByNameForWorkspaceId(nameOrCrn.getName(), workspaceId)
                : getByCrnAndWorkspaceIdAndAddToMdc(nameOrCrn.getCrn(), workspaceId);
    }

    public String getCdhVersion(@NotNull NameOrCrn nameOrCrn, Long workspaceId) {
        Blueprint blueprint = getByWorkspace(nameOrCrn, workspaceId);
        return new CmTemplateProcessor(blueprint.getBlueprintJsonText()).getStackVersion();
    }

    public void decorateWithCrn(Blueprint bp, String accountId) {
        bp.setResourceCrn(createCRN(accountId));
    }

    private Set<String> getHueHostGroups(String blueprintText) {
        return new CmTemplateProcessor(blueprintText)
                .getHostGroupsWithComponent(HueRoles.HUE_SERVER);
    }

    public RecommendationV4Response getRecommendation(Long workspaceId, String definitionName,
            String blueprintName, String credentialName, String region, String platformVariant,
            String availabilityZone, CdpResourceType cdpResourceType) {
        if (!ObjectUtils.allNotNull(region)) {
            throw new BadRequestException("region cannot be null");
        }
        PlatformRecommendation recommendation = cloudResourceAdvisor.createForBlueprint(workspaceId, definitionName, blueprintName,
                credentialName, region, platformVariant, availabilityZone, cdpResourceType);
        return platformRecommendationToPlatformRecommendationV4ResponseConverter.convert(recommendation);
    }

    public RecommendationV4Response getRecommendationByCredentialCrn(Long workspaceId, String definitionName, String blueprintName, String credentialCrn,
            String region, String platformVariant, String availabilityZone, CdpResourceType cdpResourceType) {
        PlatformRecommendation recommendation = cloudResourceAdvisor.createForBlueprintByCredCrn(workspaceId, definitionName, blueprintName,
                credentialCrn, region, platformVariant, availabilityZone, cdpResourceType);
        return platformRecommendationToPlatformRecommendationV4ResponseConverter.convert(recommendation);
    }

    public RecommendationV4Response getRecommendationByEnvironmentCrn(Long workspaceId, String definitionName, String blueprintName, String environmentCrn,
            String region, String platformVariant, String availabilityZone, CdpResourceType cdpResourceType, String architecture) {
        Credential credential = credentialClientService.getByEnvironmentCrn(environmentCrn);
        PlatformRecommendation recommendation = cloudResourceAdvisor.createForBlueprintByCred(workspaceId, definitionName, blueprintName, credential, region,
                platformVariant, availabilityZone, cdpResourceType, architecture);
        return platformRecommendationToPlatformRecommendationV4ResponseConverter.convert(recommendation);
    }

    public AutoscaleRecommendation getAutoscaleRecommendation(Long workspaceId, String blueprintName) {
        return cloudResourceAdvisor.getAutoscaleRecommendation(workspaceId, blueprintName);
    }

    public ScaleRecommendation getScaleRecommendation(Long workspaceId, String blueprintName) {
        return cloudResourceAdvisor.createForBlueprint(workspaceId, blueprintName);
    }

    public ScaleRecommendation getScaleRecommendationByDatahubCrn(Long workspaceId, String datahubCrn) {
        Blueprint blueprint = blueprintRepository.findByDatahubCrn(datahubCrn)
                .orElseThrow(NotFoundException.notFound("Blueprint by datahub crn", datahubCrn));
        return cloudResourceAdvisor.createForBlueprint(workspaceId, blueprint);
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

    public Set<Blueprint> findAllByWorkspaceWithoutUpdate(Workspace workspace) {
        return getAllAvailableInWorkspaceWithoutUpdate(workspace);
    }

    public Set<Blueprint> findAllByWorkspaceIdWithoutUpdate(Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        return getAllAvailableInWorkspaceWithoutUpdate(workspace);
    }

    public boolean anyOfTheServiceTypesPresentOnBlueprint(String blueprintText, List<String> serviceTypes) {
        try {
            CmTemplateProcessor cmTemplateProcessor = cmTemplateProcessorFactory.get(blueprintText);
            return serviceTypes.stream().anyMatch(cmTemplateProcessor::isServiceTypePresent);
        } catch (BlueprintProcessingException e) {
            LOGGER.warn("Failed to process blueprint.", e);
            return false;
        }
    }

    @Measure(BlueprintService.class)
    public Set<BlueprintView> getAllAvailableViewInWorkspaceAndFilterBySdxReady(Long workspaceId, Boolean withSdx) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        updateDefaultBlueprintCollection(workspace);
        Set<BlueprintView> blueprintViews = blueprintViewRepository.findAllByNotDeletedInWorkspace(workspaceId);
        return blueprintViews.stream()
                .filter(blueprintListFilters::isDistroXDisplayed)
                .filter(bp -> withSdx || !blueprintListFilters.isDatalakeBlueprint(bp))
                .collect(Collectors.toSet());
    }

    @Measure(BlueprintService.class)
    public Set<Blueprint> getAllAvailableInWorkspace(Workspace workspace) {
        updateDefaultBlueprintCollection(workspace);
        return getAllAvailableInWorkspaceWithoutUpdate(workspace);
    }

    @Measure(BlueprintService.class)
    public Set<Blueprint> getAllAvailableInWorkspaceWithoutUpdate(Workspace workspace) {
        return blueprintRepository.findAllByNotDeletedInWorkspace(workspace.getId());
    }

    @Measure(BlueprintService.class)
    public Blueprint getByNameForWorkspaceAndLoadDefaultsIfNecessary(String name, Workspace workspace) {
        Optional<Blueprint> blueprint = blueprintRepository.findByNameAndWorkspaceId(name, workspace.getId());
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

    @Measure(BlueprintService.class)
    public void updateDefaultBlueprintCollection(Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        updateDefaultBlueprintCollection(workspace);
    }

    private Set<Blueprint> updateDefaultBlueprintCollection(Workspace workspace) {
        Set<Blueprint> blueprintsInDatabase = blueprintRepository.findAllByWorkspaceIdAndStatusIn(workspace.getId(),
                Set.of(DEFAULT, DEFAULT_DELETED));
        if (!blueprintLoaderService.isAddingDefaultBlueprintsNecessaryForTheUser(blueprintsInDatabase)) {
            if (blueprintLoaderService.defaultBlueprintDoesNotExistInTheCache(blueprintsInDatabase)) {
                blueprintLoaderService.deleteOldDefaults(blueprintsInDatabase);
            }
            return blueprintsInDatabase;
        }
        LOGGER.debug("Modifying blueprints based on the defaults for the '{}' workspace.", workspace.getId());
        try {
            Set<Blueprint> updatedBlueprints = blueprintLoaderService.loadBlueprintsForTheWorkspace(
                    blueprintsInDatabase,
                    workspace,
                    this::saveDefaultsWithReadRight);
            LOGGER.debug("Blueprint modifications finished based on the defaults for '{}' workspace.", workspace.getId());
            return updatedBlueprints;
        } catch (ConstraintViolationException e) {
            return updateDefaultBlueprintCollection(workspace);
        }
    }

    private Optional<Blueprint> filterBlueprintsByName(String name, Collection<Blueprint> blueprints) {
        return blueprints.stream().filter(blueprint -> name.equals(blueprint.getName())).findFirst();
    }

    public boolean isDatalakeBlueprint(Blueprint blueprint) {
        return blueprintListFilters.isDatalakeBlueprint(blueprint);
    }

    private synchronized Iterable<Blueprint> saveDefaultsWithReadRight(Iterable<Blueprint> blueprints, Workspace workspace) {
        blueprints.forEach(bp -> bp.setWorkspace(workspace));
        return IterableUtils.toList(blueprints).stream()
                .map(blueprint -> {
                    // sometimes the blueprinttext is null during the save, but why?
                    try {
                        return blueprintRepository.save(blueprint);
                    } catch (DataIntegrityViolationException e) {
                        if (e.getMessage().contains("blueprintname_in_org_unique")) {
                            LOGGER.debug("Blueprint already exists in the database: {}", blueprint.getName(), e);
                        } else {
                            LOGGER.warn("Cannot update the blueprint: {}, blueprinttext: {}", blueprint.getName(), blueprint.getBlueprintJsonText(), e);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Cannot update the blueprint: {}, blueprinttext: {}", blueprint.getName(), blueprint.getBlueprintJsonText(), e);
                        throw e;
                    }
                    return blueprint;
                })
                .collect(Collectors.toList());
    }

    public boolean isClouderaManagerTemplate(Blueprint blueprint) {
        return blueprintUtils.isClouderaManagerClusterTemplate(blueprint.getBlueprintJsonText());
    }

    public String getBlueprintVariant(Blueprint blueprint) {
        return blueprintUtils.getBlueprintVariant(blueprint.getBlueprintJsonText());
    }

    @Override
    public Blueprint getByNameForWorkspace(String name, Workspace workspace) {
        return getByNameForWorkspaceAndLoadDefaultsIfNecessary(name, workspace);
    }

    @Override
    public Blueprint delete(Blueprint blueprint) {
        LOGGER.debug("Deleting blueprint with name: {}", blueprint.getName());
        prepareDeletion(blueprint);
        if (Set.of(USER_MANAGED, SERVICE_MANAGED).contains(blueprint.getStatus())) {
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
        clusterService.deleteBlueprintsOnSpecificClusters(blueprint.getId());
        Set<BaseBlueprintClusterView> blueprintClusterViews = clusterService.findByStackResourceCrn(blueprint.getId());
        if (!blueprintClusterViews.isEmpty()) {
            List<String> clusters = getClusterFromList(blueprintClusterViews);
            if (blueprintClusterViews.size() == 1) {
                checkOnlyOneRemainingEntityRelation(
                        blueprint,
                        blueprintClusterViews,
                        clusters);
            } else {
                checkOnlyMultipleRemainingEntityRelation(
                        blueprint,
                        clusters,
                        getBlueprintsFromList(blueprintClusterViews));
            }
        }
    }

    private void checkOnlyMultipleRemainingEntityRelation(Blueprint blueprint, List<String> clusters, List<Long> stackTemplateIds) {
        Set<String> clusterDefinitions = getClusterDefinitionNamesByStackTemplateIds(stackTemplateIds);
        throw new BadRequestException(String.format(
                "There are clusters or cluster definitions associated with cluster template '%s'. "
                        + "The cluster template used by %s cluster(s) (%s) and %s cluster definitions (%s). "
                        + "Please remove these before deleting the cluster template.", blueprint.getName(),
                clusters.size(), String.join(", ", clusters), clusterDefinitions.size(),
                String.join(", ", clusterDefinitions)));
    }

    private void checkOnlyOneRemainingEntityRelation(Blueprint blueprint, Set<BaseBlueprintClusterView> blueprintClusterViews, List<String> clusters) {
        BaseBlueprintClusterView blueprintClusterView = blueprintClusterViews.iterator().next();
        String clusterType = getClusterType(blueprintClusterView);
        String clusterOrClusterDefinitionName = blueprintClusterView.getName();
        throw new BadRequestException(String.format("The %s with name ['%s'] uses cluster template '%s'. Please remove the "
                        + "%s before deleting the cluster template.",
                clusterType,
                clusterOrClusterDefinitionName,
                blueprint.getName(),
                clusterType));
    }

    private List<Long> getBlueprintsFromList(Set<BaseBlueprintClusterView> blueprintClusterViews) {
        return blueprintClusterViews
                .stream()
                .filter(it -> it.getType().equals(StackType.TEMPLATE))
                .map(it -> it.getId())
                .collect(Collectors.toList());
    }

    private List<String> getClusterFromList(Set<BaseBlueprintClusterView> blueprintClusterViews) {
        return blueprintClusterViews
                .stream()
                .filter(it -> !it.getType().equals(StackType.TEMPLATE))
                .map(it -> it.getName())
                .collect(Collectors.toList());
    }

    private Set<String> getClusterDefinitionNamesByStackTemplateIds(List<Long> stackTemplateIds) {
        return clusterTemplateViewService.findAllByStackIds(stackTemplateIds)
                .stream().map(CompactView::getName)
                .collect(Collectors.toSet());
    }

    private String getClusterType(BaseBlueprintClusterView cluster) {
        return cluster.getType().equals(StackType.TEMPLATE) ? "cluster definition" : "cluster";
    }

    @Override
    protected void prepareCreation(Blueprint resource) {
        resource.setCreated(System.currentTimeMillis());
    }

    private Set<String> queryCustomParameters(String name, Long workspaceId) {
        Blueprint blueprint = getByNameForWorkspaceId(name, workspaceId);
        String blueprintText = blueprint.getBlueprintJsonText();
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
        Set<ConfigQueryEntry> result = new HashSet<>();

        Pair<Blueprint, String> bp = getBlueprintAndText(blueprintName, workspaceId);

        if (blueprintUtils.isClouderaManagerClusterTemplate(bp.getRight())) {
            FileSystemConfigQueryObject fileSystemConfigQueryObject = createFileSystemConfigQueryObject(bp, clusterName, baseLocation, fileSystemType,
                    accountName, attachedCluster, secure);
            result = cmCloudStorageConfigProvider.queryParameters(fileSystemConfigQueryObject);
        }
        return result;
    }

    public FileSystemConfigQueryObject createFileSystemConfigQueryObject(Pair<Blueprint, String> bp, String clusterName,
            String baseLocation, String fileSystemType, String accountName, boolean attachedCluster,
            boolean secure) {
        return Builder.builder()
                .withClusterName(clusterName)
                .withStorageName(StringUtils.stripEnd(baseLocation, "/"))
                .withBlueprintText(bp.getRight())
                .withFileSystemType(fileSystemType)
                .withAccountName(accountName)
                .withAttachedCluster(attachedCluster)
                .withDatalakeCluster(blueprintUtils.isSharedServiceReadyBlueprint(bp.getLeft()))
                .withSecure(secure)
                .build();
    }

    public Pair<Blueprint, String> getBlueprintAndText(String blueprintName, Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        Blueprint blueprint = getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace);
        String blueprintText = getBlueprintText(blueprintName, workspaceId);
        return Pair.of(blueprint, blueprintText);
    }

    public String getBlueprintText(String blueprintName, Long workspaceId) {
        User user = getLoggedInUser();
        Workspace workspace = getWorkspaceService().get(workspaceId, user);
        return getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace).getBlueprintJsonText();
    }

    private Blueprint getByCrnAndWorkspaceIdAndAddToMdc(String crn, Long workspaceId) {
        Blueprint bp = blueprintRepository.findByResourceCrnAndWorkspaceId(crn, workspaceId)
                .orElseThrow(() -> notFound("cluster template", crn).get());
        MDCBuilder.buildMdcContext(bp);
        return bp;
    }

    private String createCRN(String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.CLUSTER_TEMPLATE, accountId);
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return blueprintRepository.findResourceCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                .orElseThrow(NotFoundException.notFound("Blueprint", resourceName));
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return resourceNames.stream()
                .map(resourceName -> blueprintRepository.findResourceCrnByNameAndAccountId(resourceName, ThreadBasedUserCrnProvider.getAccountId())
                        .orElseThrow(NotFoundException.notFound("Blueprint", resourceName)))
                .collect(Collectors.toList());
    }

    public Blueprint getByResourceCrn(String resourceCrn) {
        return blueprintRepository.findByResourceCrn(resourceCrn);
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.CLUSTER_TEMPLATE;
    }

    public BlueprintStatusView getStatusViewByResourceCrn(String resourceCrn) {
        return blueprintViewRepository.findViewByResourceCrn(resourceCrn);
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        Map<String, Optional<String>> result = new HashMap<>();
        blueprintRepository.findResourceNamesByCrnAndAccountId(crns, ThreadBasedUserCrnProvider.getAccountId()).stream()
                .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return result;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.CLUSTER_TEMPLATE);
    }

    public Optional<Blueprint> getByClusterId(Long clusterId) {
        return blueprintRepository.findByClusterId(clusterId);
    }
}
