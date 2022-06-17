package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.CUSTOM;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.HierarchyAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4SpotParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.CloudbreakInternalCrnClient;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.imagecatalog.ImageCatalogService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.flow.service.FlowCancelService;
import com.sequenceiq.sdx.api.model.SdxAwsBase;
import com.sequenceiq.sdx.api.model.SdxAwsSpotParameters;
import com.sequenceiq.sdx.api.model.SdxAzureBase;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxCustomClusterRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;
import com.sequenceiq.sdx.api.model.SdxRecipe;

@Service
public class SdxService implements ResourceIdProvider, PayloadContextProvider, HierarchyAuthResourcePropertyProvider {

    public static final String MICRO_DUTY_REQUIRED_VERSION = "7.2.12";

    public static final String MEDIUM_DUTY_REQUIRED_VERSION = "7.2.7";

    public static final String CCMV2_JUMPGATE_REQUIRED_VERSION = "7.2.6";

    public static final String CCMV2_REQUIRED_VERSION = "7.2.1";

    public static final String SDX_RESIZE_NAME_SUFFIX = "-md";

    public static final long WORKSPACE_ID_DEFAULT = 0L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxService.class);

    @Inject
    private SdxExternalDatabaseConfigurer externalDatabaseConfigurer;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private StackV4Endpoint stackV4Endpoint;

    @Inject
    private RecipeV4Endpoint recipeV4Endpoint;

    @Inject
    private CloudbreakInternalCrnClient cloudbreakInternalCrnClient;

    @Inject
    private DistroxService distroxService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private Clock clock;

    @Inject
    private CloudStorageManifester cloudStorageManifester;

    @Inject
    private CDPConfigService cdpConfigService;

    @Inject
    private FlowCancelService flowCancelService;

    @Inject
    private OwnerAssignmentService ownerAssignmentService;

    @Inject
    private TransactionService transactionService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private DistroXV1Endpoint distroXV1Endpoint;

    @Value("${info.app.version}")
    private String sdxClusterServiceVersion;

    public List<ResourceWithId> findAsAuthorizationResorces(String accountId) {
        return sdxClusterRepository.findAuthorizationResourcesByAccountId(accountId);
    }

    public List<ResourceWithId> findAsAuthorizationResorcesByEnvName(String accountId, String envName) {
        return sdxClusterRepository.findAuthorizationResourcesByAccountIdAndEnvName(accountId, envName);
    }

    public List<ResourceWithId> findAsAuthorizationResorcesByEnvCrn(String accountId, String envCrn) {
        return sdxClusterRepository.findAuthorizationResourcesByAccountIdAndEnvCrn(accountId, envCrn);
    }

    public String getStackCrnByClusterCrn(String crn) {
        return sdxClusterRepository.findStackCrnByClusterCrn(crn)
                .orElseThrow(notFound("SdxCluster", crn));
    }

    public Set<Long> findByResourceIdsAndStatuses(Set<Long> resourceIds, Set<DatalakeStatusEnum> statuses) {
        LOGGER.info("Searching for SDX cluster by ids and statuses.");
        List<SdxStatusEntity> sdxStatusEntities = sdxStatusService.findDistinctFirstByStatusInAndDatalakeIdOrderByIdDesc(statuses, resourceIds);
        return sdxStatusEntities.stream().map(sdxStatusEntity -> sdxStatusEntity.getDatalake().getId()).collect(Collectors.toSet());
    }

    public SdxCluster getById(Long id) {
        LOGGER.info("Searching for SDX cluster by id {}", id);
        Optional<SdxCluster> sdxClusters = sdxClusterRepository.findById(id);
        if (sdxClusters.isPresent()) {
            return sdxClusters.get();
        } else {
            throw notFound("SDX cluster", id).get();
        }
    }

    public SdxCluster getByNameOrCrn(String userCrn, NameOrCrn clusterNameOrCrn) {
        return clusterNameOrCrn.hasName()
                ? getByNameInAccount(userCrn, clusterNameOrCrn.getName())
                : getByCrn(userCrn, clusterNameOrCrn.getCrn());
    }

    public Iterable<SdxCluster> findAllById(List<Long> ids) {
        return sdxClusterRepository.findAllById(ids);
    }

    public StackV4Response getDetail(String name, Set<String> entries, String accountId) {
        try {
            LOGGER.info("Calling cloudbreak for SDX cluster details by name {}", name);
            return ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> stackV4Endpoint.get(WORKSPACE_ID_DEFAULT, name, entries, accountId));
        } catch (javax.ws.rs.NotFoundException e) {
            LOGGER.info("Sdx cluster not found on CB side", e);
            return null;
        }
    }

    public SdxCluster getByCrn(String userCrn, String clusterCrn) {
        LOGGER.info("Searching for SDX cluster by crn {}", clusterCrn);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(accountIdFromCrn, clusterCrn);
        if (sdxCluster.isPresent()) {
            return sdxCluster.get();
        } else {
            throw notFound("SDX cluster", clusterCrn).get();
        }
    }

    public List<SdxCluster> getSdxClustersByCrn(String userCrn, String clusterCrn, boolean includeDeleted) {
        LOGGER.info("Searching for SDX cluster by crn {}", clusterCrn);
        List<SdxCluster> sdxClusterList = new ArrayList<>();
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(accountIdFromCrn, clusterCrn);
        if (sdxCluster.isPresent()) {
            sdxClusterList.add(sdxCluster.get());
            if (includeDeleted) {
                sdxCluster = sdxClusterRepository.findByAccountIdAndOriginalCrn(accountIdFromCrn, clusterCrn);
            } else {
                sdxCluster = sdxClusterRepository.findByAccountIdAndOriginalCrnAndDeletedIsNull(accountIdFromCrn, clusterCrn);

            }
            if (sdxCluster.isPresent()) {
                LOGGER.info("Found a detached data lake associated with crn:{}", clusterCrn);
                sdxClusterList.add(sdxCluster.get());
            }
        }
        if (sdxClusterList.isEmpty()) {
            throw notFound("SDX cluster", clusterCrn).get();
        } else {
            return sdxClusterList;
        }
    }

    public SdxCluster getByCrn(String clusterCrn) {
        LOGGER.info("Searching for SDX cluster by crn {}", clusterCrn);
        Optional<SdxCluster> sdxCluster = sdxClusterRepository.findByCrnAndDeletedIsNull(clusterCrn);
        if (sdxCluster.isPresent()) {
            return sdxCluster.get();
        } else {
            throw notFound("SDX cluster", clusterCrn).get();
        }
    }

    public String getRuntimeVersionFromImageResponse(ImageV4Response imageV4Response) {
        if (imageV4Response != null && imageV4Response.getStackDetails() != null) {
            return imageV4Response.getStackDetails() != null ? imageV4Response.getStackDetails().getVersion() : null;
        }

        return null;
    }

    public String getEnvCrnByCrn(String userCrn, String clusterCrn) {
        LOGGER.info("Searching for SDX cluster by crn {}", clusterCrn);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        Optional<String> envCrn = sdxClusterRepository.findEnvCrnByAccountIdAndCrnAndDeletedIsNull(accountIdFromCrn, clusterCrn);
        if (envCrn.isPresent()) {
            return envCrn.get();
        } else {
            throw notFound("SDX cluster", clusterCrn).get();
        }
    }

    public SdxCluster getByNameInAccount(String userCrn, String name) {
        LOGGER.info("Searching for SDX cluster by name {}", name);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        Optional<SdxCluster> sdxCluster = measure(() ->
                        sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(accountIdFromCrn, name), LOGGER,
                "Fetching SDX cluster took {}ms from DB. Name: [{}]", name);
        return sdxCluster.orElseThrow(notFound("SDX cluster", name));
    }

    public SdxCluster getByNameInAccountAllowDetached(String userCrn, String name) {
        LOGGER.info("Searching for SDX cluster by name {} allowing detached", name);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        Optional<SdxCluster> sdxCluster = measure(() ->
                        sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, name), LOGGER,
                "Fetching SDX cluster allowing detached took {}ms from DB. Name: [{}]", name);
        return sdxCluster.orElseThrow(notFound("SDX cluster", name));
    }

    public void delete(SdxCluster sdxCluster) {
        sdxClusterRepository.delete(sdxCluster);
    }

    public UpdateRecipesV4Response refreshRecipes(SdxCluster sdxCluster, UpdateRecipesV4Request request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.refreshRecipesInternal(WORKSPACE_ID_DEFAULT, request, sdxCluster.getName(), userCrn));
    }

    public AttachRecipeV4Response attachRecipe(SdxCluster sdxCluster, AttachRecipeV4Request request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.attachRecipeInternal(WORKSPACE_ID_DEFAULT, request, sdxCluster.getName(), userCrn));
    }

    public DetachRecipeV4Response detachRecipe(SdxCluster sdxCluster, DetachRecipeV4Request request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.detachRecipeInternal(WORKSPACE_ID_DEFAULT, request, sdxCluster.getName(), userCrn));
    }

    public Pair<SdxCluster, FlowIdentifier> createSdx(final String userCrn, final String name, final SdxCustomClusterRequest sdxCustomClusterRequest) {
        final Pair<SdxClusterRequest, ImageSettingsV4Request> convertedRequest = sdxCustomClusterRequest.convertToPair();

        return createSdx(userCrn, name, convertedRequest.getLeft(), null, convertedRequest.getRight());
    }

    public Pair<SdxCluster, FlowIdentifier> createSdx(final String userCrn, final String name, final SdxClusterRequest sdxClusterRequest,
            final StackV4Request internalStackV4Request) {
        return createSdx(userCrn, name, sdxClusterRequest, internalStackV4Request, null);
    }

    public void updateRangerRazEnabled(SdxCluster sdxCluster) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> {
                    RangerRazEnabledV4Response response = stackV4Endpoint.rangerRazEnabledInternal(WORKSPACE_ID_DEFAULT, sdxCluster.getCrn(), initiatorUserCrn);
                    if (response.isRangerRazEnabled()) {
                        if (!sdxCluster.isRangerRazEnabled()) {
                            validateRazEnablement(sdxCluster.getRuntime(), response.isRangerRazEnabled(),
                                    environmentClientService.getByCrn(sdxCluster.getEnvCrn()));
                            sdxCluster.setRangerRazEnabled(true);
                            save(sdxCluster);
                        }
                    } else {
                        throw new BadRequestException(String.format("Ranger raz is not installed on the datalake: %s!", sdxCluster.getClusterName()));
                    }
                });
    }

    private Pair<SdxCluster, FlowIdentifier> createSdx(final String userCrn, final String name, final SdxClusterRequest sdxClusterRequest,
            final StackV4Request internalStackV4Request, final ImageSettingsV4Request imageSettingsV4Request) {
        LOGGER.info("Creating SDX cluster with name {}", name);
        String accountId = getAccountIdFromCrn(userCrn);
        validateSdxRequest(name, sdxClusterRequest.getEnvironment(), accountId);
        DetailedEnvironmentResponse environment = validateAndGetEnvironment(sdxClusterRequest.getEnvironment());
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        ImageV4Response imageV4Response = imageCatalogService.getImageResponseFromImageRequest(imageSettingsV4Request, cloudPlatform);
        validateInternalSdxRequest(internalStackV4Request, sdxClusterRequest);
        validateRuntimeAndImage(sdxClusterRequest, environment, imageSettingsV4Request, imageV4Response);
        String runtimeVersion = getRuntime(sdxClusterRequest, internalStackV4Request, imageV4Response);
        validateCcmV2Requirement(environment, runtimeVersion);

        SdxCluster sdxCluster = validateAndCreateNewSdxCluster(userCrn,
                name,
                runtimeVersion,
                sdxClusterRequest.getClusterShape(),
                sdxClusterRequest.isEnableRangerRaz(),
                sdxClusterRequest.isEnableMultiAz(),
                environment);
        setTagsSafe(sdxClusterRequest, sdxCluster);

        if (isCloudStorageConfigured(sdxClusterRequest)) {
            validateCloudStorageRequest(sdxClusterRequest.getCloudStorage(), environment);
            String trimmedBaseLocation = StringUtils.stripEnd(sdxClusterRequest.getCloudStorage().getBaseLocation(), "/");
            sdxCluster.setCloudStorageBaseLocation(trimmedBaseLocation);
            sdxCluster.setCloudStorageFileSystemType(sdxClusterRequest.getCloudStorage().getFileSystemType());
            sdxClusterRequest.getCloudStorage().setBaseLocation(trimmedBaseLocation);
        } else if (!CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform.name()) &&
                !CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform.name()) &&
                !CloudPlatform.MOCK.equalsIgnoreCase(cloudPlatform.name()) &&
                internalStackV4Request == null) {
            throw new BadRequestException("Cloud storage parameter is required.");
        }

        externalDatabaseConfigurer.configure(cloudPlatform, sdxClusterRequest.getExternalDatabase(), sdxCluster);
        updateStackV4RequestWithEnvironmentCrnIfNotExistsOnIt(internalStackV4Request, environment.getCrn());
        StackV4Request stackRequest = getStackRequest(sdxClusterRequest.getClusterShape(), sdxClusterRequest.isEnableRangerRaz(),
                internalStackV4Request, cloudPlatform, runtimeVersion, imageSettingsV4Request);
        overrideDefaultTemplateValues(stackRequest, sdxClusterRequest.getCustomInstanceGroups(), accountId);
        validateRecipes(sdxClusterRequest, stackRequest, userCrn);
        prepareCloudStorageForStack(sdxClusterRequest, stackRequest, sdxCluster, environment);
        prepareDefaultSecurityConfigs(internalStackV4Request, stackRequest, cloudPlatform);
        prepareProviderSpecificParameters(stackRequest, sdxClusterRequest, cloudPlatform);
        updateStackV4RequestWithRecipes(sdxClusterRequest, stackRequest);
        stackRequest.setResourceCrn(sdxCluster.getCrn());
        sdxCluster.setStackRequest(stackRequest);

        MDCBuilder.buildMdcContext(sdxCluster);

        SdxCluster savedSdxCluster;
        try {
            savedSdxCluster = transactionService.required(() -> {
                SdxCluster created = sdxClusterRepository.save(sdxCluster);
                ownerAssignmentService.assignResourceOwnerRoleIfEntitled(userCrn, created.getCrn(), created.getAccountId());
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED, "Datalake requested", created);
                return created;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxCreation(savedSdxCluster);
        return Pair.of(savedSdxCluster, flowIdentifier);
    }

    private void overrideDefaultTemplateValues(StackV4Request defaultTemplate, List<SdxInstanceGroupRequest> customInstanceGroups, String accountId) {
        if (isCustomInstanceTypeSelected(customInstanceGroups) && !entitlementService.isDatalakeSelectInstanceTypeEnabled(accountId)) {
            throw new BadRequestException("Datalake instance type selection is not enabled! " +
                    "Contact Cloudera support to enable CDP_DATALAKE_SELECT_INSTANCE_TYPE entitlement for the account.");
        }
        if (CollectionUtils.isNotEmpty(customInstanceGroups)) {
            LOGGER.debug("Override default template with custom instance groups from request.");
            customInstanceGroups.forEach(customInstanceGroup -> {
                InstanceGroupV4Request templateInstanceGroup = defaultTemplate.getInstanceGroups().stream()
                        .filter(templateGroup -> templateGroup.getName() != null && templateGroup.getName().equals(customInstanceGroup.getName()))
                        .findAny()
                        .orElseThrow(() -> new BadRequestException("Custom instance group is missing from default template: " + customInstanceGroup.getName()));
                overrideInstanceType(templateInstanceGroup, customInstanceGroup.getInstanceType());
            });
        }
    }

    private boolean isCustomInstanceTypeSelected(List<SdxInstanceGroupRequest> customInstanceGroups) {
        return CollectionUtils.isNotEmpty(customInstanceGroups)
                && customInstanceGroups.stream().anyMatch(customInstanceGroup -> StringUtils.isNotEmpty(customInstanceGroup.getInstanceType()));
    }

    private void overrideInstanceType(InstanceGroupV4Request templateGroup, String newInstanceType) {
        InstanceTemplateV4Request instanceTemplate = templateGroup.getTemplate();
        if (instanceTemplate != null && StringUtils.isNoneBlank(newInstanceType)) {
            LOGGER.debug("Override instance group {} instance type from {} to {}",
                    templateGroup.getName(), instanceTemplate.getInstanceType(), newInstanceType);
            instanceTemplate.setInstanceType(newInstanceType);
        }
    }

    private void validateRecipes(SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request, String userCrn) {
        Set<SdxRecipe> recipes = sdxClusterRequest.getRecipes();
        if (CollectionUtils.isNotEmpty(recipes)) {
            List<InstanceGroupV4Request> igs = stackV4Request.getInstanceGroups();
            Set<String> recipeNames = fetchRecipesFromCore();
            recipes.forEach(recipe -> {
                validateRecipeExists(recipeNames, recipe);
                validateInstanceGroupExistsForRecipe(igs, recipe);
            });
        }
    }

    private Set<String> fetchRecipesFromCore() {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        RecipeViewV4Responses recipeResponses = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> recipeV4Endpoint.listInternal(
                        WORKSPACE_ID_DEFAULT, initiatorUserCrn));
        Set<String> recipeNames = recipeResponses.getResponses()
                .stream()
                .map(CompactViewV4Response::getName).collect(Collectors.toSet());
        return recipeNames;
    }

    private void validateRecipeExists(Set<String> recipeNames, SdxRecipe recipe) {
        if (!recipeNames.contains(recipe.getName())) {
            throw new NotFoundException(String.format("Not found recipe with name %s", recipe.getName()));
        }
        LOGGER.debug("Found recipe with name {}", recipe.getName());
    }

    private void validateInstanceGroupExistsForRecipe(List<InstanceGroupV4Request> igs, SdxRecipe recipe) {
        boolean foundIg = igs.stream().anyMatch(ig -> recipe.getHostGroup().equals(ig.getName()));
        if (!foundIg) {
            throw new NotFoundException(String.format("Not found instance group with name %s for recipe %s",
                    recipe.getHostGroup(), recipe.getName()));
        }
    }

    public Pair<SdxCluster, FlowIdentifier> resizeSdx(final String userCrn, final String clusterName, final SdxClusterResizeRequest sdxClusterResizeRequest) {
        LOGGER.info("Re-sizing SDX cluster with name {}", clusterName);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        String environmentName = sdxClusterResizeRequest.getEnvironment();
        SdxClusterShape shape = sdxClusterResizeRequest.getClusterShape();

        final SdxCluster sdxCluster = sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(accountIdFromCrn, clusterName)
                .orElseThrow(() -> notFound("SDX cluster", clusterName).get());

        MDCBuilder.buildMdcContext(sdxCluster);

        validateSdxResizeRequest(sdxCluster, accountIdFromCrn, shape);
        StackV4Response stackV4Response = getDetail(clusterName,
                Set.of(StackResponseEntries.HARDWARE_INFO.getEntryName(), StackResponseEntries.EVENTS.getEntryName()), accountIdFromCrn);

        DetailedEnvironmentResponse environment = validateAndGetEnvironment(environmentName);

        SdxCluster newSdxCluster = validateAndCreateNewSdxCluster(userCrn,
                clusterName,
                sdxCluster.getRuntime(),
                shape,
                sdxCluster.isRangerRazEnabled(),
                sdxCluster.isEnableMultiAz(),
                environment);
        newSdxCluster.setTags(sdxCluster.getTags());
        newSdxCluster.setCrn(sdxCluster.getCrn());
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        if (!StringUtils.isBlank(sdxCluster.getCloudStorageBaseLocation())) {
            newSdxCluster.setCloudStorageBaseLocation(sdxCluster.getCloudStorageBaseLocation());
            newSdxCluster.setCloudStorageFileSystemType(sdxCluster.getCloudStorageFileSystemType());
        } else if (!CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform.name()) &&
                !CloudPlatform.GCP.equalsIgnoreCase(cloudPlatform.name()) &&
                !CloudPlatform.MOCK.equalsIgnoreCase(cloudPlatform.name())) {
            throw new BadRequestException("Cloud storage parameter is required.");
        }

        newSdxCluster.setDatabaseAvailabilityType(sdxCluster.getDatabaseAvailabilityType());
        newSdxCluster.setDatabaseEngineVersion(sdxCluster.getDatabaseEngineVersion());
        StackV4Request stackRequest = getStackRequest(shape, sdxCluster.isRangerRazEnabled(), null, cloudPlatform, sdxCluster.getRuntime(), null);
        if (shape == SdxClusterShape.MEDIUM_DUTY_HA) {
            // This is added to make sure the host name used by Light and Medium duty are not the same.
            CustomDomainSettingsV4Request customDomainSettingsV4Request = new CustomDomainSettingsV4Request();
            customDomainSettingsV4Request.setHostname(sdxCluster.getClusterName() + SDX_RESIZE_NAME_SUFFIX);
            stackRequest.setCustomDomain(customDomainSettingsV4Request);
        }

        prepareCloudStorageForStack(stackRequest, stackV4Response, newSdxCluster, environment);
        prepareDefaultSecurityConfigs(null, stackRequest, cloudPlatform);
        try {
            if (!StringUtils.isBlank(sdxCluster.getStackRequestToCloudbreak())) {
                StackV4Request stackV4RequestOrig = JsonUtil.readValue(sdxCluster.getStackRequestToCloudbreak(), StackV4Request.class);
                stackRequest.setImage(stackV4RequestOrig.getImage());
            }
        } catch (IOException ioException) {
            LOGGER.error("Failed to re-use the image catalog. Will use default catalog", ioException);
        }
        stackRequest.setResourceCrn(newSdxCluster.getCrn());
        newSdxCluster.setStackRequest(stackRequest);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxResize(sdxCluster.getId(), newSdxCluster);
        return Pair.of(sdxCluster, flowIdentifier);
    }

    public SdxCluster refreshDataHub(String clusterName, String datahubName) {
        if (!entitlementService.isDatalakeLightToMediumMigrationEnabled(ThreadBasedUserCrnProvider.getAccountId())) {
            throw new BadRequestException("Refresh of the data hub is not supported");
        }
        String accountIdFromCrn = getAccountIdFromCrn(ThreadBasedUserCrnProvider.getUserCrn());
        SdxCluster sdxCluster = sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, clusterName)
                .orElseThrow(() -> notFound("SDX cluster", clusterName).get());
        if (Strings.isNullOrEmpty(datahubName)) {
            distroxService.restartAttachedDistroxClusters(sdxCluster.getEnvCrn());
        } else {
            StackV4Response stackV4Response = distroXV1Endpoint.getByName(datahubName, null);
            distroxService.restartDistroxByCrns(List.of(stackV4Response.getCrn()));
        }
        return sdxCluster;
    }

    private SdxCluster validateAndCreateNewSdxCluster(String userCrn,
            String clusterName,
            String runtime,
            SdxClusterShape shape,
            boolean razEnabled,
            boolean enableMultiAz,
            DetailedEnvironmentResponse environmentResponse) {
        validateShape(shape, runtime, environmentResponse);
        validateRazEnablement(runtime, razEnabled, environmentResponse);
        validateMultiAz(enableMultiAz, environmentResponse);
        SdxCluster newSdxCluster = new SdxCluster();
        newSdxCluster.setCrn(createCrn(getAccountIdFromCrn(userCrn)));
        newSdxCluster.setClusterName(clusterName);
        newSdxCluster.setAccountId(getAccountIdFromCrn(userCrn));
        newSdxCluster.setClusterShape(shape);
        newSdxCluster.setCreated(clock.getCurrentTimeMillis());
        newSdxCluster.setEnvName(environmentResponse.getName());
        newSdxCluster.setEnvCrn(environmentResponse.getCrn());
        newSdxCluster.setSdxClusterServiceVersion(sdxClusterServiceVersion);
        newSdxCluster.setRangerRazEnabled(razEnabled);
        newSdxCluster.setRuntime(runtime);
        newSdxCluster.setEnableMultiAz(enableMultiAz);
        return newSdxCluster;
    }

    private void validateMultiAz(boolean enableMultiAz, DetailedEnvironmentResponse environmentResponse) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (enableMultiAz) {
            if (!environmentResponse.getCloudPlatform().equals(AWS.name())) {
                validationBuilder.error("Provisioning a multi AZ cluster is only enabled for AWS.");
            }
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }

    }

    private void validateEnv(DetailedEnvironmentResponse environment) {
        if (environment.getEnvironmentStatus().isDeleteInProgress()) {
            throw new BadRequestException("The environment is in delete in progress phase. Please create a new environment first!");
        } else if (environment.getEnvironmentStatus().isStopInProgressOrStopped()) {
            throw new BadRequestException("The environment is stopped. Please start the environment first!");
        } else if (environment.getEnvironmentStatus().isStartInProgress()) {
            throw new BadRequestException("The environment is starting. Please wait until finished!");
        } else if (environment.getEnvironmentStatus().isFailed()) {
            throw new BadRequestException("The environment is in failed phase. Please fix the environment or create a new one first!");
        }
    }

    private StackV4Request getStackRequest(SdxClusterShape shape, boolean razEnabled, StackV4Request internalStackV4Request, CloudPlatform cloudPlatform,
            String runtimeVersion, ImageSettingsV4Request imageSettingsV4Request) {
        if (internalStackV4Request == null) {
            StackV4Request stackRequest = cdpConfigService.getConfigForKey(
                    new CDPConfigKey(cloudPlatform, shape, runtimeVersion));
            if (stackRequest == null) {
                LOGGER.error("Can't find template for cloudplatform: {}, shape {}, cdp version: {}", cloudPlatform, shape, runtimeVersion);
                throw new BadRequestException("Can't find template for cloudplatform: " + cloudPlatform + ", shape: " + shape +
                        ", runtime version: " + runtimeVersion);
            }
            stackRequest.getCluster().setRangerRazEnabled(razEnabled);

            if (imageSettingsV4Request != null) {
                stackRequest.setImage(imageSettingsV4Request);
            }

            return stackRequest;
        } else {
            // We have provided a --ranger-raz-enabled flag in the CLI, but it will
            // get overwritten if you use a custom json (using --cli-json). To avoid
            // this, we will set the raz enablement here as well. See CB-7474 for more details
            internalStackV4Request.getCluster().setRangerRazEnabled(razEnabled);
            return internalStackV4Request;
        }
    }

    private String getRuntime(SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request, ImageV4Response imageV4Response) {
        if (imageV4Response != null) {
            return getRuntimeVersionFromImageResponse(imageV4Response);
        } else if (sdxClusterRequest.getRuntime() != null) {
            return sdxClusterRequest.getRuntime();
        } else if (stackV4Request != null) {
            return null;
        } else {
            return cdpConfigService.getDefaultRuntime();
        }
    }

    private void updateStackV4RequestWithRecipes(SdxClusterRequest clusterRequest, StackV4Request stackV4Request) {
        Set<SdxRecipe> recipes = clusterRequest.getRecipes();
        if (CollectionUtils.isNotEmpty(recipes)) {
            List<InstanceGroupV4Request> instanceGroups = stackV4Request.getInstanceGroups();
            instanceGroups.forEach(ig -> {
                Set<String> recipeNamesForIg = recipes.stream()
                        .filter(r -> r.getHostGroup().equals(ig.getName()))
                        .map(SdxRecipe::getName).collect(Collectors.toSet());
                ig.setRecipeNames(recipeNamesForIg);
            });
        }
    }

    private void updateStackV4RequestWithEnvironmentCrnIfNotExistsOnIt(StackV4Request request, String environmentCrn) {
        if (request != null && StringUtils.isEmpty(request.getEnvironmentCrn())) {
            request.setEnvironmentCrn(environmentCrn);
            LOGGER.debug("Environment crn for internal sdx stack request set to: {}", environmentCrn);
        }
    }

    private void prepareCloudStorageForStack(SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request,
            SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        CloudStorageRequest cloudStorageRequest = cloudStorageManifester.initCloudStorageRequest(environment,
                stackV4Request.getCluster(), sdxCluster, sdxClusterRequest);
        stackV4Request.getCluster().setCloudStorage(cloudStorageRequest);
    }

    private void prepareCloudStorageForStack(StackV4Request stackV4Request, StackV4Response stackV4Response,
            SdxCluster sdxCluster, DetailedEnvironmentResponse environment) {
        CloudStorageRequest cloudStorageRequest = cloudStorageManifester.initCloudStorageRequestFromExistingSdxCluster(environment,
                stackV4Response.getCluster(), sdxCluster);
        stackV4Request.getCluster().setCloudStorage(cloudStorageRequest);
    }

    public FlowIdentifier sync(String name, String accountId) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.sync(WORKSPACE_ID_DEFAULT, name, accountId));
    }

    public void syncByCrn(String userCrn, String crn) {
        SdxCluster sdxCluster = getByCrn(userCrn, crn);
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> stackV4Endpoint.sync(WORKSPACE_ID_DEFAULT, sdxCluster.getClusterName(), Crn.fromString(crn).getAccountId()));
    }

    public FlowIdentifier syncComponentVersionsFromCm(String userCrn, NameOrCrn clusterNameOrCrn) {
        SdxCluster cluster = getByNameOrCrn(userCrn, clusterNameOrCrn);
        MDCBuilder.buildMdcContext(cluster);
        SdxStatusEntity sdxStatus = sdxStatusService.getActualStatusForSdx(cluster);
        if (sdxStatus.getStatus().isStopState()) {
            String message = String.format("Reading CM and parcel versions from CM cannot be initiated as the datalake is in %s state", sdxStatus.getStatus());
            LOGGER.info(message);
            throw new BadRequestException(message);
        } else {
            LOGGER.info("Syncing CM and parcel versions from CM initiated");
            return sdxReactorFlowManager.triggerDatalakeSyncComponentVersionsFromCmFlow(cluster);
        }
    }

    protected StackV4Request prepareDefaultSecurityConfigs(StackV4Request internalRequest, StackV4Request stackV4Request, CloudPlatform cloudPlatform) {
        if (internalRequest == null && !List.of("MOCK", "YARN").contains(cloudPlatform.name())) {
            stackV4Request.getInstanceGroups().forEach(instance -> {
                SecurityGroupV4Request groupRequest = new SecurityGroupV4Request();
                if (InstanceGroupType.CORE.equals(instance.getType())) {
                    groupRequest.setSecurityRules(rulesWithPorts("22"));
                } else if (InstanceGroupType.GATEWAY.equals(instance.getType())) {
                    groupRequest.setSecurityRules(rulesWithPorts("443", "22"));
                } else {
                    throw new IllegalStateException("Unknown instance group type " + instance.getType());
                }
                instance.setSecurityGroup(groupRequest);
            });
        }
        return stackV4Request;
    }

    private List<SecurityRuleV4Request> rulesWithPorts(String... ports) {
        return Stream.of(ports)
                .map(port -> {
                    SecurityRuleV4Request ruleRequest = new SecurityRuleV4Request();
                    ruleRequest.setSubnet("0.0.0.0/0");
                    ruleRequest.setPorts(List.of(port));
                    ruleRequest.setProtocol("tcp");
                    return ruleRequest;
                })
                .collect(Collectors.toList());
    }

    private void prepareProviderSpecificParameters(StackV4Request stackRequest, SdxClusterRequest sdxClusterRequest, CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AWS:
                useAwsSpotPercentageIfPresent(stackRequest, sdxClusterRequest);
                break;
            case AZURE:
                updateAzureLoadBalancerSkuIfPresent(stackRequest, sdxClusterRequest);
                break;
            case GCP:
            case YARN:
            case MOCK:
            default:
                break;
        }
    }

    private void useAwsSpotPercentageIfPresent(StackV4Request stackRequest, SdxClusterRequest sdxClusterRequest) {
        Optional.ofNullable(sdxClusterRequest.getAws())
                .map(SdxAwsBase::getSpot)
                .ifPresent(spotParameters -> updateAwsSpotParameters(stackRequest, spotParameters));
    }

    private void updateAwsSpotParameters(StackV4Request stackRequest, SdxAwsSpotParameters sdxSpotParameters) {
        stackRequest.getInstanceGroups().stream()
                .map(InstanceGroupV4Request::getTemplate)
                .peek(template -> {
                    if (template.getAws() == null) {
                        template.setAws(new AwsInstanceTemplateV4Parameters());
                    }
                })
                .map(InstanceTemplateV4Base::getAws)
                .peek(aws -> {
                    if (aws.getSpot() == null) {
                        aws.setSpot(new AwsInstanceTemplateV4SpotParameters());
                    }
                })
                .map(AwsInstanceTemplateV4Parameters::getSpot)
                .forEach(spot -> {
                    spot.setPercentage(sdxSpotParameters.getPercentage());
                    spot.setMaxPrice(sdxSpotParameters.getMaxPrice());
                });
    }

    private void updateAzureLoadBalancerSkuIfPresent(StackV4Request stackRequest, SdxClusterRequest sdxClusterRequest) {
        Optional.ofNullable(sdxClusterRequest.getAzure())
                .map(SdxAzureBase::getLoadBalancerSku)
                .ifPresent(sku -> {
                    AzureStackV4Parameters azureParameters = stackRequest.createAzure();
                    azureParameters.setLoadBalancerSku(sku);
                    stackRequest.setAzure(azureParameters);
                });
    }

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return getByCrn(userCrn, resourceCrn).getId();
    }

    @Override
    public List<Long> getResourceIdsByResourceCrn(String resourceCrn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return getSdxClustersByCrn(userCrn, resourceCrn, true).stream().map(cluster -> cluster.getId()).collect(Collectors.toList());
    }

    @Override
    public Long getResourceIdByResourceName(String resourceName) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        return getByNameInAccount(userCrn, resourceName).getId();
    }

    private void validateCloudStorageRequest(SdxCloudStorageRequest cloudStorage, DetailedEnvironmentResponse environment) {
        if (cloudStorage != null) {
            ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
            validationBuilder.ifError(() -> cloudStorage.getFileSystemType() == null, "'fileSystemType' must be set in 'cloudStorage'!");
            validationBuilder.merge(validateBaseLocation(cloudStorage.getBaseLocation()));

            if (StringUtils.isEmpty(cloudStorage.getBaseLocation())) {
                validationBuilder.error("'baseLocation' must be set in 'cloudStorage'!");
            } else {
                if (FileSystemType.S3.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.S3.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'S3'!", FileSystemType.S3.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getS3() == null, "'s3' must be set if 'fileSystemType' is 'S3'!");
                }
                if (FileSystemType.ADLS.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.ADLS.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'ADLS'!", FileSystemType.ADLS.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getAdls() == null, "'adls' must be set if 'fileSystemType' is 'ADLS'!");
                }
                if (FileSystemType.ADLS_GEN_2.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.ADLS_GEN_2.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'ADLS_GEN_2'!",
                                    FileSystemType.ADLS_GEN_2.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getAdlsGen2() == null, "'adlsGen2' must be set if 'fileSystemType' is 'ADLS_GEN_2'!");
                }
                if (FileSystemType.WASB.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.WASB.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'WASB'", FileSystemType.WASB.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getWasb() == null, "'wasb' must be set if 'fileSystemType' is 'WASB'!");
                }
                if (FileSystemType.GCS.equals(cloudStorage.getFileSystemType())) {
                    validationBuilder.ifError(() -> !cloudStorage.getBaseLocation().startsWith(FileSystemType.GCS.getProtocol()),
                            String.format("'baseLocation' must start with '%s' if 'fileSystemType' is 'GCS'!", FileSystemType.GCS.getProtocol()));
                    validationBuilder.ifError(() -> cloudStorage.getGcs() == null, "'gcs' must be set if 'fileSystemType' is 'GCS'!");
                }
            }

            ValidationResult validationResult = validationBuilder.build();
            if (validationResult.hasError()) {
                throw new BadRequestException(validationResult.getFormattedErrors());
            }
        }
    }

    ValidationResult validateBaseLocation(String baseLocation) {
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();
        if (baseLocation != null) {
            Pattern pattern = Pattern.compile("\\s");
            Matcher matcher = pattern.matcher(baseLocation.trim());
            if (matcher.find()) {
                resultBuilder.error("You have added some whitespace to the base location: " + baseLocation);
            }
        } else {
            LOGGER.debug("Cannot validate the base location, because it's null");
        }
        return resultBuilder.build();
    }

    private void validateRazEnablement(String runtime, boolean razEnabled, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (razEnabled) {
            CloudPlatform cloudPlatform = EnumUtils.getEnumIgnoreCase(CloudPlatform.class, environment.getCloudPlatform());
            if (!(AWS.equals(cloudPlatform) || AZURE.equals(cloudPlatform))) {
                validationBuilder.error("Provisioning Ranger Raz is only valid for Amazon Web Services and Microsoft Azure.");
            }
            if (!isRazSupported(runtime, cloudPlatform)) {
                String errorMsg = AWS.equals(cloudPlatform) ?
                        "Provisioning Ranger Raz on Amazon Web Services is only valid for CM version greater than or equal to 7.2.2 and not " :
                        "Provisioning Ranger Raz on Microsoft Azure is only valid for CM version greater than or equal to 7.2.1 and not ";
                validationBuilder.error(errorMsg + runtime);
            }
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateShape(SdxClusterShape shape, String runtime, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (SdxClusterShape.MICRO_DUTY.equals(shape)) {
            if (!entitlementService.microDutySdxEnabled(Crn.safeFromString(environment.getCreator()).getAccountId())) {
                validationBuilder.error(String.format("Provisioning a micro duty data lake cluster is not enabled for %s. " +
                        "Contact Cloudera support to enable CDP_MICRO_DUTY_SDX entitlement for the account.", environment.getCloudPlatform()));
            }
            if (!isShapeVersionSupportedByRuntime(runtime, MICRO_DUTY_REQUIRED_VERSION)) {
                validationBuilder.error("Provisioning a Micro Duty SDX shape is only valid for CM version greater than or equal to "
                        + MICRO_DUTY_REQUIRED_VERSION + " and not " + runtime);
            }
        } else if (SdxClusterShape.MEDIUM_DUTY_HA.equals(shape)) {
            if (!isShapeVersionSupportedByRuntime(runtime, MEDIUM_DUTY_REQUIRED_VERSION)) {
                validationBuilder.error("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                        + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + runtime);
            }
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private boolean isShapeVersionSupportedByRuntime(String runtime, String shapeVersion) {
        // If runtime is empty, then SDX internal call was used, so we assume it's supported.
        if (StringUtils.isEmpty(runtime)) {
            return true;
        }
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, () -> shapeVersion) > -1;
    }

    /**
     * Ranger Raz is only on 7.2.1 and later on Microsoft Azure, and only on 7.2.2 and later on Amazon Web Services.
     * If runtime is empty, then sdx-internal call was used.
     */
    private boolean isRazSupported(String runtime, CloudPlatform cloudPlatform) {
        if (StringUtils.isEmpty(runtime)) {
            return true;
        }
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, () -> AWS.equals(cloudPlatform) ? "7.2.2" : "7.2.1") > -1;
    }

    private boolean isCloudStorageConfigured(SdxClusterRequest clusterRequest) {
        return clusterRequest.getCloudStorage() != null
                && isNotEmpty(clusterRequest.getCloudStorage().getBaseLocation());
    }

    private void validateSdxRequest(String name, String envName, String accountId) {
        sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(accountId, name)
                .ifPresent(foundSdx -> {
                    throw new BadRequestException("SDX cluster exists with this name: " + name);
                });

        sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(accountId, envName).stream().findFirst()
                .ifPresent(existedSdx -> {
                    throw new BadRequestException("SDX cluster exists for environment name: " + existedSdx.getEnvName());
                });
    }

    private void validateSdxResizeRequest(SdxCluster sdxCluster, String accountId, SdxClusterShape shape) {
        if (!entitlementService.isDatalakeLightToMediumMigrationEnabled(accountId)) {
            throw new BadRequestException("Resizing of the data lake is not supported");
        }
        if (sdxCluster.getClusterShape() == shape) {
            throw new BadRequestException("SDX cluster already is of requested shape");
        }
        sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(accountId, sdxCluster.getEnvCrn())
                .ifPresent(existedSdx -> {
                    throw new BadRequestException("SDX which is detached already exists for the environment. SDX name: " + existedSdx.getClusterName());
                });
        if (sdxCluster.getCloudStorageFileSystemType() != null && sdxCluster.getCloudStorageFileSystemType().isGcs()) {
            throw new BadRequestException("Unsupported cloud provider GCP. SDX name: " + sdxCluster.getClusterName());
        }
    }

    private void validateRuntimeAndImage(SdxClusterRequest clusterRequest, DetailedEnvironmentResponse environment,
            ImageSettingsV4Request imageSettingsV4Request, ImageV4Response imageV4Response) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        CloudPlatform cloudPlatform = EnumUtils.getEnumIgnoreCase(CloudPlatform.class, environment.getCloudPlatform());

        if (imageV4Response != null) {
            if (clusterRequest.getRuntime() != null) {
                validationBuilder.error("SDX cluster request must not specify both runtime version and image at the same time because image " +
                        "decides runtime version.");
            }
        } else if (imageSettingsV4Request != null && clusterRequest.getRuntime() == null) {
            if (cloudPlatform.equals(CloudPlatform.MOCK)) {
                clusterRequest.setRuntime(MEDIUM_DUTY_REQUIRED_VERSION);
            } else {
                validationBuilder.error("SDX cluster request has null runtime version and null image response. It cannot " +
                        "determine the runtime version.");
            }
        }

        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateInternalSdxRequest(StackV4Request stackv4Request, SdxClusterRequest sdxClusterRequest) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (stackv4Request != null) {
            SdxClusterShape clusterShape = sdxClusterRequest.getClusterShape();
            if (!clusterShape.equals(CUSTOM)) {
                validationResultBuilder.error("Cluster shape '" + clusterShape + "' is not accepted on SDX Internal API. Use 'CUSTOM' cluster shape");
            }
            if (stackv4Request.getCluster() == null) {
                validationResultBuilder.error("Cluster cannot be null.");
            }
            if (CollectionUtils.isNotEmpty(sdxClusterRequest.getCustomInstanceGroups())) {
                validationResultBuilder.error("Custom instance group is not accepted on SDX Internal API.");
            }
        }
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            LOGGER.error("Cannot create SDX via internal API: {}", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateCcmV2Requirement(DetailedEnvironmentResponse environment, String runtimeVersion) {
        Comparator<Versioned> versionComparator = new VersionComparator();

        if (environment.getTunnel() != null && runtimeVersion != null) {
            switch (environment.getTunnel()) {
                case CCMV2:
                    if (versionComparator.compare(() -> CCMV2_REQUIRED_VERSION, () -> runtimeVersion) > 0) {
                        throw new BadRequestException(String.format("Runtime version %s does not support Cluster Connectivity Manager. " +
                                "Please try creating a datalake with runtime version at least %s.", runtimeVersion, CCMV2_REQUIRED_VERSION));
                    }
                    break;
                case CCMV2_JUMPGATE:
                    if (versionComparator.compare(() -> CCMV2_JUMPGATE_REQUIRED_VERSION, () -> runtimeVersion) > 0) {
                        throw new BadRequestException(String.format("Runtime version %s does not support Cluster Connectivity Manager. " +
                                "Please try creating a datalake with runtime version at least %s.", runtimeVersion, CCMV2_JUMPGATE_REQUIRED_VERSION));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void setTagsSafe(SdxClusterRequest sdxClusterRequest, SdxCluster sdxCluster) {
        try {
            if (sdxClusterRequest.getTags() == null) {
                sdxCluster.setTags(new Json(new HashMap<>()));
            } else {
                sdxCluster.setTags(new Json(sdxClusterRequest.getTags()));
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Can not convert tags", e);
        }
    }

    public List<SdxCluster> listSdxByEnvCrn(String userCrn, String envCrn) {
        LOGGER.info("Listing SDX clusters by environment crn {}", envCrn);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        return sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsFalse(accountIdFromCrn, envCrn);
    }

    public List<SdxCluster> listAllSdxByEnvCrn(String userCrn, String envCrn) {
        LOGGER.info("Listing all the SDX clusters by environment crn {}", envCrn);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        return sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNull(accountIdFromCrn, envCrn);
    }

    public List<SdxCluster> listSdxByEnvCrn(String envCrn) {
        LOGGER.debug("Listing SDX clusters by environment crn {}", envCrn);
        String accountIdFromCrn = getAccountIdFromCrn(envCrn);
        return sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsFalse(accountIdFromCrn, envCrn);
    }

    public List<SdxCluster> listSdx(String userCrn, String envName) {
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        if (envName != null) {
            LOGGER.info("Listing SDX clusters by environment name {}", envName);
            return sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNullAndDetachedIsFalse(accountIdFromCrn, envName);
        } else {
            return sdxClusterRepository.findByAccountIdAndDeletedIsNullAndDetachedIsFalse(accountIdFromCrn);
        }
    }

    public List<SdxCluster> listAllSdx(String userCrn, String envName) {
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        if (envName != null) {
            LOGGER.info("Listing all the SDX clusters by environment name {}", envName);
            return sdxClusterRepository.findByAccountIdAndEnvNameAndDeletedIsNull(accountIdFromCrn, envName);
        } else {
            return sdxClusterRepository.findByAccountIdAndDeletedIsNull(accountIdFromCrn);
        }
    }

    public FlowIdentifier deleteSdxByClusterCrn(String userCrn, String clusterCrn, boolean forced) {
        LOGGER.info("Deleting SDX {}", clusterCrn);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        return sdxClusterRepository.findByAccountIdAndCrnAndDeletedIsNull(accountIdFromCrn, clusterCrn)
                .map(sdxCluster -> deleteSdxCluster(sdxCluster, forced))
                .orElseThrow(() -> notFound("SDX cluster", clusterCrn).get());
    }

    public FlowIdentifier deleteSdx(String userCrn, String name, boolean forced) {
        LOGGER.info("Deleting SDX {}", name);
        String accountIdFromCrn = getAccountIdFromCrn(userCrn);
        return sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, name)
                .map(sdxCluster -> deleteSdxCluster(sdxCluster, forced))
                .orElseThrow(() -> notFound("SDX cluster", name).get());
    }

    public Optional<String> updateRuntimeVersionFromStackResponse(SdxCluster sdxCluster, StackV4Response stackV4Response) {
        String clusterName = sdxCluster.getClusterName();
        Optional<String> cdpVersionOpt = getCdpVersion(stackV4Response);
        LOGGER.info("Update '{}' runtime version from stackV4Response", clusterName);
        if (cdpVersionOpt.isPresent()) {
            String version = cdpVersionOpt.get();
            LOGGER.info("Update Sdx runtime version of {} to {}, previous version: {}", clusterName, version, sdxCluster.getRuntime());
            sdxCluster.setRuntime(version);
            sdxClusterRepository.save(sdxCluster);
        } else {
            LOGGER.warn("Cannot update the Sdx runtime version for cluster: {}", clusterName);
        }
        return cdpVersionOpt;
    }

    private Optional<String> getCdpVersion(StackV4Response stack) {
        String stackName = stack.getName();
        ClusterV4Response cluster = stack.getCluster();
        if (cluster != null) {
            ClouderaManagerV4Response cm = cluster.getCm();
            if (cm != null) {
                LOGGER.info("Repository details are available for cluster: {}: {}", stackName, cm);
                List<ClouderaManagerProductV4Response> products = cm.getProducts();
                if (products != null && !products.isEmpty()) {
                    Optional<ClouderaManagerProductV4Response> cdpOpt = products.stream().filter(p -> "CDH".equals(p.getName())).findFirst();
                    if (cdpOpt.isPresent()) {
                        return getRuntimeVersionFromCdpVersion(cdpOpt.get().getVersion());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> getRuntimeVersionFromCdpVersion(String cdpVersion) {
        if (isNotEmpty(cdpVersion)) {
            LOGGER.info("Extract runtime version from CDP version: {}", cdpVersion);
            return Optional.of(StringUtils.substringBefore(cdpVersion, "-"));
        }
        return Optional.empty();
    }

    private FlowIdentifier deleteSdxCluster(SdxCluster sdxCluster, boolean forced) {
        checkIfSdxIsDeletable(sdxCluster, forced);
        MDCBuilder.buildMdcContext(sdxCluster);
        sdxClusterRepository.save(sdxCluster);
        sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.DELETE_REQUESTED, "Datalake deletion requested", sdxCluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxDeletion(sdxCluster, forced);
        flowCancelService.cancelRunningFlows(sdxCluster.getId());
        return flowIdentifier;
    }

    private void checkIfSdxIsDeletable(SdxCluster sdxCluster, boolean forced) {
        if (!forced && sdxCluster.hasExternalDatabase() && StringUtils.isEmpty(sdxCluster.getDatabaseCrn())) {
            throw new BadRequestException(String.format("Can not find external database for Data Lake, but it was requested: %s. Please use force delete.",
                    sdxCluster.getClusterName()));
        }
        Collection<StackViewV4Response> attachedDistroXClusters = Collections.emptyList();
        try {
            attachedDistroXClusters = distroxService.getAttachedDistroXClusters(sdxCluster.getEnvCrn());
        } catch (Exception ex) {
            if (!forced) {
                throw ex;
            }
        }

        // If there are no attached data hubs, we can just return from this function. Nothing else to check.
        if (attachedDistroXClusters.isEmpty()) {
            return;
        }

        Collection<StackViewV4Response> runningDistroXClusters = attachedDistroXClusters.stream()
                .filter(cluster -> !cluster.getStatus().isStopped()).collect(Collectors.toList());

        if (!runningDistroXClusters.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Hub cluster(s) must be stopped before attempting SDX deletion [%s].",
                    runningDistroXClusters.stream().map(StackViewV4Response::getName).collect(Collectors.joining(", "))));
        }

        // If we reach here, it means that all the DistroX clusters are stopped. We will allow SDX delete if and only if
        // the "force" option is used.
        if (!forced) {
            throw new BadRequestException(String.format("The following stopped Data Hubs clusters(s) must be terminated " +
                            "before SDX deleting [%s]. Use --force to skip this check.",
                    attachedDistroXClusters.stream().map(StackViewV4Response::getName).collect(Collectors.joining(", "))));
        }
    }

    private String createCrn(@Nonnull String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.DATALAKE, accountId);
    }

    public String getAccountIdFromCrn(String crnStr) {
        try {
            Crn crn = Crn.safeFromString(crnStr);
            return crn.getAccountId();
        } catch (NullPointerException | CrnParseException e) {
            throw new BadRequestException("Can not parse CRN to find account ID: " + crnStr);
        }
    }

    private DetailedEnvironmentResponse validateAndGetEnvironment(String environmentName) {
        DetailedEnvironmentResponse environmentResponse = environmentClientService.getByName(environmentName);
        validateEnv(environmentResponse);
        return environmentResponse;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        return getByNameInAccountAllowDetached(ThreadBasedUserCrnProvider.getUserCrn(), resourceName).getCrn();
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        return resourceNames.stream()
                .map(resourceName -> getByNameInAccount(ThreadBasedUserCrnProvider.getUserCrn(), resourceName).getCrn())
                .collect(Collectors.toList());
    }

    @Override
    public PayloadContext getPayloadContext(Long resourceId) {
        try {
            SdxCluster sdxCluster = getById(resourceId);
            DetailedEnvironmentResponse envResp = environmentClientService.getByCrn(sdxCluster.getEnvCrn());
            return PayloadContext.create(sdxCluster.getCrn(), envResp.getCloudPlatform());
        } catch (NotFoundException ignored) {
            // skip
        } catch (Exception e) {
            LOGGER.warn("Error happened during fetching payload context for datalake with environment response.", e);
        }
        return null;
    }

    @Override
    public Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        try {
            return Optional.of(getEnvCrnByCrn(ThreadBasedUserCrnProvider.getUserCrn(), resourceCrn));
        } catch (NotFoundException e) {
            LOGGER.error(String.format("Getting environment crn by resource crn %s failed, ", resourceCrn), e);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Optional<String>> getEnvironmentCrnsByResourceCrns(Collection<String> resourceCrns) {
        Set<String> resourceCrnSet = new LinkedHashSet<>(resourceCrns);
        List<SdxCluster> clusters = sdxClusterRepository.findAllByAccountIdAndCrnAndDeletedIsNullAndDetachedIsFalse(
                getAccountIdFromCrn(ThreadBasedUserCrnProvider.getUserCrn()),
                resourceCrnSet);
        Map<String, Optional<String>> resourceCrnWithEnvCrn = new LinkedHashMap<>();
        clusters.forEach(cluster -> resourceCrnWithEnvCrn.put(cluster.getCrn(), Optional.ofNullable(cluster.getEnvCrn())));
        return resourceCrnWithEnvCrn;
    }

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.DATALAKE;
    }

    public SdxCluster save(SdxCluster sdxCluster) {
        return sdxClusterRepository.save(sdxCluster);
    }

    public void updateCertExpirationState(Long id, CertExpirationState state) {
        sdxClusterRepository.updateCertExpirationState(id, state);
    }

    @Override
    public Map<String, Optional<String>> getNamesByCrnsForMessage(Collection<String> crns) {
        Map<String, Optional<String>> result = new HashMap<>();
        sdxClusterRepository.findResourceNamesByCrnAndAccountId(crns, ThreadBasedUserCrnProvider.getAccountId())
                .forEach(nameAndCrn -> result.put(nameAndCrn.getCrn(), Optional.ofNullable(nameAndCrn.getName())));
        return result;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.DATALAKE);
    }

    public Set<String> getInstanceGroupNamesBySdxDetails(SdxClusterShape clusterShape, String runtimeVersion, String cloudPlatform) {
        if (clusterShape == null || StringUtils.isAnyBlank(runtimeVersion, cloudPlatform)) {
            throw new BadRequestException("The following query params needs to be filled for this request: clusterShape, runtimeVersion, cloudPlatform");
        }
        Set<String> result = new HashSet<>();
        StackV4Request stackV4Request = cdpConfigService.getConfigForKey(new CDPConfigKey(
                CloudPlatform.valueOf(cloudPlatform), clusterShape, runtimeVersion
        ));
        if (stackV4Request != null && CollectionUtils.isNotEmpty(stackV4Request.getInstanceGroups())) {
            result = stackV4Request.getInstanceGroups().stream().map(InstanceGroupV4Base::getName).collect(Collectors.toSet());
        }
        return result;
    }
}
