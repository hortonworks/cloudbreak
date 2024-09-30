package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFoundException;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.CALLER_ID_NOT_FOUND;
import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.CDP_CALLER_ID_HEADER;
import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.USER_AGENT_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderValueProvider.getHeaderOrItsFallbackValueOrDefault;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.HierarchyAuthResourcePropertyProvider;
import com.sequenceiq.authorization.service.OwnerAssignmentService;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.CompactViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceTemplateV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4SpotParameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.RotateSaltPasswordRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.event.PayloadContext;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.vm.VirtualMachineConfiguration;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.common.model.ImageCatalogPlatform;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxDatabaseRepository;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.datalake.service.imagecatalog.ImageCatalogService;
import com.sequenceiq.datalake.service.sdx.database.DatabaseParameterFallbackUtil;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.sdx.flowcheck.CloudbreakFlowService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.PayloadContextProvider;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.sdx.api.model.SdxAwsBase;
import com.sequenceiq.sdx.api.model.SdxAwsSpotParameters;
import com.sequenceiq.sdx.api.model.SdxAzureBase;
import com.sequenceiq.sdx.api.model.SdxCloudStorageRequest;
import com.sequenceiq.sdx.api.model.SdxClusterRequest;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseComputeStorageRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupDiskRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;
import com.sequenceiq.sdx.api.model.SdxRecipe;
import com.sequenceiq.sdx.api.model.SdxRefreshDatahubResponse;

@Service
public class SdxService implements ResourceIdProvider, PayloadContextProvider, HierarchyAuthResourcePropertyProvider {

    public static final String MICRO_DUTY_REQUIRED_VERSION = "7.2.12";

    public static final String MEDIUM_DUTY_REQUIRED_VERSION = "7.2.7";

    public static final String MEDIUM_DUTY_MAXIMUM_VERSION = "7.2.17";

    public static final String ENTERPRISE_DATALAKE_REQUIRED_VERSION = "7.2.17";

    public static final String CCMV2_JUMPGATE_REQUIRED_VERSION = "7.2.6";

    public static final String CCMV2_REQUIRED_VERSION = "7.2.1";

    public static final long WORKSPACE_ID_DEFAULT = 0L;

    public static final String INSTANCE_TYPE = "instancetype";

    public static final String STORAGE = "storage";

    public static final String PREVIOUS_DATABASE_CRN = "previousDatabaseCrn";

    public static final String PREVIOUS_CLUSTER_SHAPE = "previousClusterShape";

    public static final String DATABASE_SSL_ENABLED = "databaseSslEnabled";

    public static final Map<CloudPlatform, Versioned> MIN_RUNTIME_VERSION_FOR_RAZ = new HashMap<>() {
        {
            put(AWS, () -> "7.2.2");
            put(AZURE, () -> "7.2.2");
            put(GCP, () -> "7.2.17");
        }
    };

    public static final Versioned MIN_RUNTIME_VERSION_FOR_RMS = () -> "7.2.18";

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

    @Inject
    private CloudbreakFlowService cloudbreakFlowService;

    @Inject
    private PlatformConfig platformConfig;

    @Inject
    private VirtualMachineConfiguration virtualMachineConfiguration;

    @Inject
    private PlatformStringTransformer platformStringTransformer;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private SdxDatabaseRepository sdxDatabaseRepository;

    @Inject
    private CloudbreakPoller cloudbreakPoller;

    @Value("${info.app.version}")
    private String sdxClusterServiceVersion;

    @Inject
    private SdxRecommendationService sdxRecommendationService;

    @Inject
    private PlatformAwareSdxConnector platformAwareSdxConnector;

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
        return sdxClusterRepository.findStackCrnByClusterCrn(Crn.safeFromString(crn).getAccountId(), crn)
                .orElseThrow(notFound("SdxCluster", crn));
    }

    public Set<Long> findByResourceIdsAndStatuses(Set<Long> resourceIds, Set<DatalakeStatusEnum> statuses) {
        LOGGER.info("Searching for SDX cluster by ids: {} and statuses: {}", resourceIds, statuses);
        List<SdxStatusEntity> sdxStatusEntities = sdxStatusService.findLatestSdxStatusesFilteredByStatusesAndDatalakeIds(statuses, resourceIds);
        return sdxStatusEntities.stream().map(sdxStatusEntity -> sdxStatusEntity.getDatalake().getId()).collect(Collectors.toSet());
    }

    public Optional<String> findResourceCrnById(Long id) {
        LOGGER.debug("Trying to fetch resource CRN based on the following {} id: {}", SdxCluster.class.getSimpleName(), id);
        Optional<String> resourceCrn = sdxClusterRepository.findCrnById(id);
        resourceCrn.ifPresentOrElse(s -> LOGGER.debug("Resource CRN has found [for stack id: {}]: {}", id, s),
                () -> LOGGER.debug("No resource CRN has been found for stack id: {}", id));
        return resourceCrn;
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
        } catch (jakarta.ws.rs.NotFoundException e) {
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
        }
        if (includeDeleted) {
            sdxClusterList.addAll(sdxClusterRepository.findByAccountIdAndOriginalCrn(accountIdFromCrn, clusterCrn));
        } else {
            sdxClusterList.addAll(sdxClusterRepository.findByAccountIdAndOriginalCrnAndDeletedIsNull(accountIdFromCrn, clusterCrn));
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

    public Pair<SdxCluster, FlowIdentifier> createSdx(final String userCrn, final String name, final SdxClusterRequest sdxClusterRequest,
            final StackV4Request internalStackV4Request) {
        ImageSettingsV4Request imageSettingsV4Request = getImageSettingsV4Request(sdxClusterRequest, internalStackV4Request);
        return createSdx(userCrn, name, sdxClusterRequest, internalStackV4Request, imageSettingsV4Request);
    }

    private ImageSettingsV4Request getImageSettingsV4Request(SdxClusterRequest sdxClusterRequest, StackV4Request internalStackV4Request) {
        ImageSettingsV4Request imageSettingsV4Request = null;
        if (internalStackV4Request != null && internalStackV4Request.getImage() != null) {
            imageSettingsV4Request = internalStackV4Request.getImage();
        } else if (sdxClusterRequest.getImage() != null) {
            imageSettingsV4Request = sdxClusterRequest.getImage();
        }
        if (StringUtils.isNotBlank(sdxClusterRequest.getOs())) {
            if (imageSettingsV4Request == null) {
                imageSettingsV4Request = new ImageSettingsV4Request();
                imageSettingsV4Request.setOs(sdxClusterRequest.getOs());
            } else if (!StringUtils.equalsIgnoreCase(imageSettingsV4Request.getOs(), sdxClusterRequest.getOs())) {
                throw new BadRequestException("Differing os was set in request, only the image settings os should be set.");
            }
        }
        return imageSettingsV4Request;
    }

    public void updateRangerRazEnabled(SdxCluster sdxCluster) {
        String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
        ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> {
                    RangerRazEnabledV4Response response = stackV4Endpoint.rangerRazEnabledInternal(WORKSPACE_ID_DEFAULT, sdxCluster.getCrn(), initiatorUserCrn);
                    if (response.isRangerRazEnabled()) {
                        if (!sdxCluster.isRangerRazEnabled()) {
                            DetailedEnvironmentResponse environmentResponse = environmentClientService.getByCrn(sdxCluster.getEnvCrn());
                            validateRazEnablement(sdxCluster.getRuntime(), response.isRangerRazEnabled(),
                                    environmentResponse);
                            sdxCluster.setRangerRazEnabled(true);
                            save(sdxCluster);
                        }
                    } else {
                        throw new BadRequestException(String.format("Ranger raz is not installed on the datalake: %s!", sdxCluster.getClusterName()));
                    }
                });
    }

    private Pair<SdxCluster, FlowIdentifier> createSdx(final String userCrn, final String name, final SdxClusterRequest sdxClusterRequest,
            final StackV4Request internalStackV4Request, ImageSettingsV4Request imageSettingsV4Request) {
        LOGGER.info("Creating SDX cluster with name {}", name);
        String accountId = getAccountIdFromCrn(userCrn);
        validateSdxRequest(name, sdxClusterRequest.getEnvironment(), accountId);
        validateJavaVersion(sdxClusterRequest.getJavaVersion(), accountId);
        DetailedEnvironmentResponse environment = validateAndGetEnvironment(sdxClusterRequest.getEnvironment());
        platformAwareSdxConnector.validateIfOtherPlatformsHasSdx(environment.getCrn(), TargetPlatform.PAAS);
        validateImageRequest(sdxClusterRequest, imageSettingsV4Request);
        ImageCatalogPlatform imageCatalogPlatform = platformStringTransformer
                .getPlatformStringForImageCatalog(environment.getCloudPlatform(), isGovCloudEnvironment(environment));
        ImageV4Response imageV4Response = imageCatalogService.getImageResponseFromImageRequest(imageSettingsV4Request, imageCatalogPlatform);
        validateInternalSdxRequest(internalStackV4Request, sdxClusterRequest);
        validateRuntimeAndImage(sdxClusterRequest, environment, imageSettingsV4Request, imageV4Response);
        String runtimeVersion = getRuntime(sdxClusterRequest, internalStackV4Request, imageV4Response);
        String os = getOs(sdxClusterRequest, internalStackV4Request, imageV4Response);

        validateCcmV2Requirement(environment, runtimeVersion);

        SdxCluster sdxCluster = validateAndCreateNewSdxCluster(sdxClusterRequest, runtimeVersion, name, userCrn, environment);
        setTagsSafe(sdxClusterRequest, sdxCluster);

        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        if (isCloudStorageConfigured(sdxClusterRequest)) {
            validateCloudStorageRequest(sdxClusterRequest.getCloudStorage(), environment);
            String trimmedBaseLocation = StringUtils.stripEnd(sdxClusterRequest.getCloudStorage().getBaseLocation(), "/");
            sdxCluster.setCloudStorageBaseLocation(trimmedBaseLocation);
            sdxCluster.setCloudStorageFileSystemType(sdxClusterRequest.getCloudStorage().getFileSystemType());
            sdxClusterRequest.getCloudStorage().setBaseLocation(trimmedBaseLocation);
        } else if (!CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform.name()) &&
                !GCP.equalsIgnoreCase(cloudPlatform.name()) &&
                !MOCK.equalsIgnoreCase(cloudPlatform.name()) &&
                internalStackV4Request == null) {
            throw new BadRequestException("Cloud storage parameter is required.");
        }

        DatabaseRequest internalDatabaseRequest = Optional.ofNullable(internalStackV4Request).map(StackV4Request::getExternalDatabase).orElse(null);
        sdxCluster.setSdxDatabase(externalDatabaseConfigurer.configure(environment, os, internalDatabaseRequest,
                sdxClusterRequest.getExternalDatabase(), sdxCluster));

        updateStackV4RequestWithEnvironmentCrnIfNotExistsOnIt(internalStackV4Request, environment.getCrn());
        StackV4Request stackRequest = getStackRequest(sdxClusterRequest.getClusterShape(), internalStackV4Request,
                cloudPlatform, runtimeVersion, imageSettingsV4Request);
        setStackRequestParams(stackRequest, sdxClusterRequest.getJavaVersion(), sdxClusterRequest.isEnableRangerRaz(), sdxClusterRequest.isEnableRangerRms());

        overrideDefaultInstanceType(stackRequest, sdxClusterRequest.getCustomInstanceGroups(), Collections.emptyList(), Collections.emptyList(),
                sdxClusterRequest.getClusterShape());
        validateRecipes(sdxClusterRequest, stackRequest, userCrn);
        prepareCloudStorageForStack(sdxClusterRequest, stackRequest, sdxCluster, environment);
        prepareDefaultSecurityConfigs(sdxClusterRequest.getClusterShape(), stackRequest, cloudPlatform);
        prepareProviderSpecificParameters(stackRequest, sdxClusterRequest, cloudPlatform);
        updateStackV4RequestWithRecipes(sdxClusterRequest, stackRequest);
        String resourceCrn = sdxCluster.getCrn();
        stackRequest.setResourceCrn(resourceCrn);
        sdxCluster.setStackRequest(stackRequest);

        MDCBuilder.buildMdcContext(sdxCluster);

        ownerAssignmentService.assignResourceOwnerRoleIfEntitled(userCrn, resourceCrn);

        SdxCluster savedSdxCluster;
        validateAndGetEnvironment(sdxClusterRequest.getEnvironment());
        try {
            savedSdxCluster = transactionService.required(() -> {
                SdxCluster created = save(sdxCluster);
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.REQUESTED, "Datalake requested", created);
                return created;
            });
        } catch (TransactionExecutionException e) {
            ownerAssignmentService.notifyResourceDeleted(resourceCrn);
            throw new TransactionRuntimeExecutionException(e);
        }
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxCreation(savedSdxCluster);
        return Pair.of(savedSdxCluster, flowIdentifier);
    }

    private void overrideDefaultInstanceType(StackV4Request defaultTemplate, List<SdxInstanceGroupRequest> customInstanceGroups,
            List<InstanceGroupV4Request> originalInstanceGroups, List<InstanceGroupV4Response> currentInstanceGroups, SdxClusterShape sdxClusterShape) {
        if (CollectionUtils.isNotEmpty(customInstanceGroups)) {
            LOGGER.debug("Override default template with custom instance groups from request.");
            customInstanceGroups.forEach(customInstanceGroup -> {
                InstanceGroupV4Request templateInstanceGroup = getTemplateInstanceGroup(defaultTemplate, customInstanceGroup.getName())
                        .orElseThrow(() -> new BadRequestException("Custom instance group is missing from default template: " + customInstanceGroup.getName()));
                overrideInstanceType(templateInstanceGroup, customInstanceGroup.getInstanceType());
            });
        } else if (CollectionUtils.isNotEmpty(originalInstanceGroups) && CollectionUtils.isNotEmpty(currentInstanceGroups)
                && !SdxClusterShape.LIGHT_DUTY.equals(sdxClusterShape)) {
            LOGGER.debug("Override default template with previous instance groups");
            currentInstanceGroups.forEach(currentInstanceGroup -> {
                originalInstanceGroups
                        .stream()
                        .filter(templateGroup -> StringUtils.equals(templateGroup.getName(), currentInstanceGroup.getName()))
                        .findAny()
                        .ifPresent(originalInstanceGroup -> overrideDefaultInstanceTypeFromPreviousDatalake(defaultTemplate, currentInstanceGroup,
                                originalInstanceGroup));
            });
        }
    }

    private void overrideDefaultInstanceTypeFromPreviousDatalake(StackV4Request defaultTemplate, InstanceGroupV4Response currentInstanceGroup,
            InstanceGroupV4Request originalInstanceGroup) {
        if (currentInstanceGroup != null && currentInstanceGroup.getTemplate() != null && originalInstanceGroup.getTemplate() != null &&
                !currentInstanceGroup.getTemplate().getInstanceType().equals(originalInstanceGroup.getTemplate().getInstanceType())) {
            getTemplateInstanceGroup(defaultTemplate, currentInstanceGroup.getName())
                    .ifPresent(templateIg -> overrideInstanceType(templateIg, currentInstanceGroup.getTemplate().getInstanceType()));
        }
    }

    private boolean isCustomInstanceTypeSelected(List<SdxInstanceGroupRequest> customInstanceGroups) {
        return CollectionUtils.isNotEmpty(customInstanceGroups)
                && customInstanceGroups.stream().anyMatch(customInstanceGroup -> StringUtils.isNotEmpty(customInstanceGroup.getInstanceType()));
    }

    private void overrideInstanceType(InstanceGroupV4Request templateGroup, String newInstanceType) {
        InstanceTemplateV4Request instanceTemplate = templateGroup.getTemplate();
        if (instanceTemplate != null && StringUtils.isNoneBlank(newInstanceType)) {
            LOGGER.info("Override instance group {} instance type from {} to {}",
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
        return recipeResponses.getResponses()
                .stream()
                .map(CompactViewV4Response::getName).collect(Collectors.toSet());
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

        if (sdxClusterResizeRequest.isValidationOnly() && sdxClusterResizeRequest.isSkipValidation()) {
            throw new BadRequestException("The Validation Only flag cannot be used with the SkipValidation flag");
        }

        final SdxCluster sdxCluster = sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(accountIdFromCrn, clusterName)
                .orElseThrow(() -> notFound("SDX cluster", clusterName).get());

        MDCBuilder.buildMdcContext(sdxCluster);

        validateSdxResizeRequest(sdxCluster, accountIdFromCrn, shape, sdxClusterResizeRequest.isEnableMultiAz());
        StackV4Response stackV4Response = getDetail(clusterName,
                Set.of(StackResponseEntries.HARDWARE_INFO.getEntryName(), StackResponseEntries.EVENTS.getEntryName()), accountIdFromCrn);

        DetailedEnvironmentResponse environment = validateAndGetEnvironment(environmentName);

        SdxCluster newSdxCluster = validateAndCreateNewSdxClusterForResize(sdxCluster, shape, sdxCluster.isEnableMultiAz()
                || sdxClusterResizeRequest.isEnableMultiAz(), clusterName, userCrn, environment);
        newSdxCluster.setTags(sdxCluster.getTags());
        newSdxCluster.setCrn(sdxCluster.getCrn());
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        if (!StringUtils.isBlank(sdxCluster.getCloudStorageBaseLocation())) {
            newSdxCluster.setCloudStorageBaseLocation(sdxCluster.getCloudStorageBaseLocation());
            newSdxCluster.setCloudStorageFileSystemType(sdxCluster.getCloudStorageFileSystemType());
        } else if (!CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform.name()) &&
                !GCP.equalsIgnoreCase(cloudPlatform.name()) &&
                !MOCK.equalsIgnoreCase(cloudPlatform.name())) {
            throw new BadRequestException("Cloud storage parameter is required.");
        }

        newSdxCluster.setSdxDatabase(DatabaseParameterFallbackUtil.setupDatabaseInitParams(sdxCluster.getDatabaseAvailabilityType(),
                sdxCluster.getDatabaseEngineVersion(), Optional.ofNullable(sdxCluster.getSdxDatabase()).map(SdxDatabase::getAttributes).orElse(null)));
        StackV4Request stackRequest = getStackRequest(shape, null, cloudPlatform, sdxCluster.getRuntime(), null);
        setStackRequestParams(stackRequest, stackV4Response.getJavaVersion(), sdxCluster.isRangerRazEnabled(), sdxCluster.isRangerRmsEnabled());

        setRecipesFromStackV4ResponseToStackV4Request(stackV4Response, stackRequest);
        CustomDomainSettingsV4Request customDomainSettingsV4Request = new CustomDomainSettingsV4Request();
        String azSuffix = (shape == sdxCluster.getClusterShape()) ? "-az" : "";
        customDomainSettingsV4Request.setHostname(sdxCluster.getClusterName() + shape.getResizeSuffix() + azSuffix);
        stackRequest.setCustomDomain(customDomainSettingsV4Request);

        prepareCloudStorageForStack(stackRequest, stackV4Response, newSdxCluster);
        prepareDefaultSecurityConfigs(null, stackRequest, cloudPlatform);
        prepareImageForStack(stackRequest, stackV4Response);

        stackRequest.setResourceCrn(newSdxCluster.getCrn());
        List<InstanceGroupV4Request> originalInstanceGroups = getInstanceGroupsByCDPConfig(sdxCluster.getClusterShape(), cloudPlatform, sdxCluster.getRuntime());
        overrideDefaultInstanceType(stackRequest, sdxClusterResizeRequest.getCustomInstanceGroups(), originalInstanceGroups,
                stackV4Response.getInstanceGroups(), sdxCluster.getClusterShape());
        overrideDefaultInstanceStorage(stackRequest, sdxClusterResizeRequest.getCustomInstanceGroupDiskSize(), stackV4Response.getInstanceGroups(),
                sdxCluster.getClusterShape());
        overrideDefaultDatabaseProperties(newSdxCluster.getSdxDatabase(), sdxClusterResizeRequest.getCustomSdxDatabaseComputeStorage(),
                sdxCluster.getSdxDatabase().getDatabaseCrn(), sdxCluster.getClusterShape(), stackV4Response.getCluster().isDbSSLEnabled());
        stackRequest.setNetwork(createNetworkRequestFromCurrentDatalake(stackV4Response.getNetwork()));
        newSdxCluster.setStackRequest(stackRequest);
        sdxRecommendationService.validateVmTypeOverride(environment, newSdxCluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxResize(sdxCluster.getId(), newSdxCluster,
                new DatalakeDrSkipOptions(sdxClusterResizeRequest.isSkipValidation(), sdxClusterResizeRequest.isSkipAtlasMetadata(),
                        sdxClusterResizeRequest.isSkipRangerAudits(), sdxClusterResizeRequest.isSkipRangerMetadata()),
                sdxClusterResizeRequest.isValidationOnly());
        return Pair.of(sdxCluster, flowIdentifier);
    }

    private void setRecipesFromStackV4ResponseToStackV4Request(StackV4Response stackV4Response, StackV4Request stackRequest) {
        Map<String, Set<String>> instanceGroupNameRecipesMap = stackV4Response.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> CollectionUtils.isNotEmpty(instanceGroup.getRecipes()))
                .collect(Collectors.toMap(InstanceGroupV4Response::getName,
                        instanceGroup -> instanceGroup.getRecipes()
                                .stream()
                                .map(RecipeV4Base::getName)
                                .collect(Collectors.toSet())));
        stackRequest.getInstanceGroups()
                .stream()
                .filter(ig -> instanceGroupNameRecipesMap.containsKey(ig.getName()))
                .forEach(
                        ig -> {
                            Set<String> recipeNames = instanceGroupNameRecipesMap.get(ig.getName());
                            ig.setRecipeNames(recipeNames);
                        }
                );
    }

    private void overrideDefaultInstanceStorage(StackV4Request defaultTemplate, List<SdxInstanceGroupDiskRequest> customInstanceGroupDisks,
            List<InstanceGroupV4Response> currentInstanceGroups, SdxClusterShape sdxClusterShape) {
        if (CollectionUtils.isNotEmpty(customInstanceGroupDisks)) {
            LOGGER.debug("Override default template with custom instance groups from request.");
            customInstanceGroupDisks.forEach(customInstanceGroupDisk -> {
                InstanceGroupV4Request templateInstanceGroup =
                        getTemplateInstanceGroup(defaultTemplate, customInstanceGroupDisk.getName())
                                .orElseThrow(() -> new BadRequestException("Custom instance group is missing from default template: "
                                        + customInstanceGroupDisk.getName()));
                overrideInstanceStorage(templateInstanceGroup, customInstanceGroupDisk.getInstanceDiskSize());
            });
        } else if (CollectionUtils.isNotEmpty(currentInstanceGroups) && !SdxClusterShape.LIGHT_DUTY.equals(sdxClusterShape)) {
            LOGGER.debug("Override default template with modified instance groups from previous datalake.");
            currentInstanceGroups.forEach(instanceGroupDisk -> {
                Optional<InstanceGroupV4Request> templateInstanceGroupOp = getTemplateInstanceGroup(defaultTemplate, instanceGroupDisk.getName());
                overrideInstanceStorageWithPreviousDiskSize(instanceGroupDisk, templateInstanceGroupOp);
            });
        }
    }

    private Optional<InstanceGroupV4Request> getTemplateInstanceGroup(StackV4Request defaultTemplate, String instanceName) {
        return defaultTemplate
                .getInstanceGroups()
                .stream()
                .filter(templateGroup -> StringUtils.equals(templateGroup.getName(), instanceName))
                .findAny();
    }

    private void overrideInstanceStorageWithPreviousDiskSize(InstanceGroupV4Response instanceGroupDisk,
            Optional<InstanceGroupV4Request> templateInstanceGroupOp) {
        if (templateInstanceGroupOp.isPresent()) {
            int previousStorageSize = instanceGroupDisk
                    .getTemplate()
                    .getAttachedVolumes()
                    .stream()
                    .filter(vol -> vol.getSize() != null)
                    .mapToInt(VolumeV4Response::getSize)
                    .max()
                    .orElse(0);
            int templateStorageSize = templateInstanceGroupOp
                    .get()
                    .getTemplate()
                    .getAttachedVolumes()
                    .stream()
                    .filter(vol -> vol.getSize() != null)
                    .mapToInt(VolumeV4Request::getSize)
                    .max()
                    .orElse(0);
            if (previousStorageSize > templateStorageSize) {
                overrideInstanceStorage(templateInstanceGroupOp.get(), previousStorageSize);
            }
        }
    }

    private void overrideInstanceStorage(InstanceGroupV4Request templateGroup, Integer storageSize) {
        InstanceTemplateV4Request instanceTemplate = templateGroup.getTemplate();
        if (instanceTemplate != null && storageSize > 0) {
            instanceTemplate
                    .getAttachedVolumes()
                    .forEach(volumeV4Request -> {
                        LOGGER.info("Override instance group {} storage size from {} to {}",
                                templateGroup.getName(), volumeV4Request.getSize(), storageSize);
                        volumeV4Request.setSize(storageSize);
                    });
        }
    }

    private void overrideDefaultDatabaseProperties(SdxDatabase sdxDatabase, SdxDatabaseComputeStorageRequest customSdxDatabase,
            String previousDatabaseCrn, SdxClusterShape previousClusterShape, boolean dbSSLEnabled) {
        Map<String, Object> attributes = sdxDatabase.getAttributes() != null ? sdxDatabase.getAttributes().getMap() : new HashMap<>();
        if (customSdxDatabase != null) {
            LOGGER.info("Custom database properties: {}", customSdxDatabase);
            if (isNotEmpty(customSdxDatabase.getInstanceType())) {
                attributes.put(INSTANCE_TYPE, customSdxDatabase.getInstanceType());
            }
            if (customSdxDatabase.getStorageSize() != null && customSdxDatabase.getStorageSize() > 0) {
                attributes.put(STORAGE, String.valueOf(customSdxDatabase.getStorageSize()));
            }
        }
        attributes.put(PREVIOUS_DATABASE_CRN, previousDatabaseCrn);
        attributes.put(PREVIOUS_CLUSTER_SHAPE, previousClusterShape.toString());
        attributes.put(DATABASE_SSL_ENABLED, dbSSLEnabled);
        sdxDatabase.setAttributes(new Json(attributes));
    }

    public SdxRefreshDatahubResponse refreshDataHub(String clusterName, String datahubName) {
        SdxRefreshDatahubResponse response = new SdxRefreshDatahubResponse();
        String accountIdFromCrn = getAccountIdFromCrn(ThreadBasedUserCrnProvider.getUserCrn());
        SdxCluster sdxCluster = sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNull(accountIdFromCrn, clusterName)
                .orElseThrow(() -> notFound("SDX cluster", clusterName).get());
        if (Strings.isNullOrEmpty(datahubName)) {
            distroxService.restartAttachedDistroxClusters(sdxCluster.getEnvCrn());
            distroxService.getAttachedDistroXClusters(sdxCluster.getEnvCrn())
                    .forEach(response::addStack);
            return response;
        }
        StackV4Response stackV4Response = distroXV1Endpoint.getByName(datahubName, null);
        distroxService.restartDistroxByCrns(List.of(stackV4Response.getCrn()));
        response.addStack(stackV4Response);
        return response;
    }

    private void setStackRequestParams(StackV4Request stackV4Request, Integer javaVersion, boolean razEnabled, boolean rmsEnabled) {
        if (javaVersion != null) {
            stackV4Request.setJavaVersion(javaVersion);
        }
        // We have provided a --ranger-raz-enabled flag in the CLI, but it will
        // get overwritten if you use a custom json (using --cli-json). To avoid
        // this, we will set the raz enablement here. See CB-7474 for more details
        stackV4Request.getCluster().setRangerRazEnabled(razEnabled);
        stackV4Request.getCluster().setRangerRmsEnabled(rmsEnabled);
    }

    private SdxCluster validateAndCreateNewSdxClusterForResize(SdxCluster sdxCluster, SdxClusterShape shape, boolean enableMultiAz,
            String clusterName, String userCrn, DetailedEnvironmentResponse environmentResponse) {
        validateShape(shape, sdxCluster.getRuntime(), environmentResponse);
        validateRazEnablement(sdxCluster.getRuntime(), sdxCluster.isRangerRazEnabled(), environmentResponse);
        validateRmsEnablement(sdxCluster.getRuntime(), sdxCluster.isRangerRazEnabled(), sdxCluster.isRangerRmsEnabled(),
                environmentResponse.getCloudPlatform(), environmentResponse.getAccountId());
        validateMultiAz(enableMultiAz, environmentResponse, shape);
        SdxCluster newSdxCluster = new SdxCluster();
        newSdxCluster.setCrn(createCrn(getAccountIdFromCrn(userCrn)));
        newSdxCluster.setClusterName(clusterName);
        newSdxCluster.setAccountId(getAccountIdFromCrn(userCrn));
        newSdxCluster.setClusterShape(shape);
        newSdxCluster.setCreated(clock.getCurrentTimeMillis());
        newSdxCluster.setEnvName(environmentResponse.getName());
        newSdxCluster.setEnvCrn(environmentResponse.getCrn());
        newSdxCluster.setSdxClusterServiceVersion(sdxClusterServiceVersion);
        newSdxCluster.setRangerRazEnabled(sdxCluster.isRangerRazEnabled());
        newSdxCluster.setRangerRmsEnabled(sdxCluster.isRangerRmsEnabled());
        newSdxCluster.setRuntime(sdxCluster.getRuntime());
        newSdxCluster.setEnableMultiAz(enableMultiAz);
        newSdxCluster.setCreatorClient(getHeaderOrItsFallbackValueOrDefault(USER_AGENT_HEADER, CDP_CALLER_ID_HEADER, CALLER_ID_NOT_FOUND));
        return newSdxCluster;
    }

    private SdxCluster validateAndCreateNewSdxCluster(SdxClusterRequest cluster, String version, String clusterName, String userCrn,
            DetailedEnvironmentResponse environmentResponse) {
        validateShape(cluster.getClusterShape(), version, environmentResponse);
        validateRazEnablement(version, cluster.isEnableRangerRaz(), environmentResponse);
        validateRmsEnablement(version, cluster.isEnableRangerRaz(), cluster.isEnableRangerRms(),
                environmentResponse.getCloudPlatform(), environmentResponse.getAccountId());
        validateMultiAz(cluster.isEnableMultiAz(), environmentResponse, cluster.getClusterShape());
        SdxCluster newSdxCluster = new SdxCluster();
        newSdxCluster.setCrn(createCrn(getAccountIdFromCrn(userCrn)));
        newSdxCluster.setClusterName(clusterName);
        newSdxCluster.setAccountId(getAccountIdFromCrn(userCrn));
        newSdxCluster.setClusterShape(cluster.getClusterShape());
        newSdxCluster.setCreated(clock.getCurrentTimeMillis());
        newSdxCluster.setEnvName(environmentResponse.getName());
        newSdxCluster.setEnvCrn(environmentResponse.getCrn());
        newSdxCluster.setSdxClusterServiceVersion(sdxClusterServiceVersion);
        newSdxCluster.setRangerRazEnabled(cluster.isEnableRangerRaz());
        newSdxCluster.setRangerRmsEnabled(cluster.isEnableRangerRms());
        newSdxCluster.setRuntime(version);
        newSdxCluster.setEnableMultiAz(cluster.isEnableMultiAz());
        newSdxCluster.setCreatorClient(getHeaderOrItsFallbackValueOrDefault(USER_AGENT_HEADER, CDP_CALLER_ID_HEADER, CALLER_ID_NOT_FOUND));
        return newSdxCluster;
    }

    private void validateMultiAz(boolean enableMultiAz, DetailedEnvironmentResponse environmentResponse, SdxClusterShape clusterShape) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (enableMultiAz) {
            CloudPlatform cloudPlatform = CloudPlatform.valueOf(environmentResponse.getCloudPlatform());
            Set<CloudPlatform> multiAzSupportedPlatforms = platformConfig.getMultiAzSupportedPlatforms();
            if (!multiAzSupportedPlatforms.contains(cloudPlatform)) {
                validationBuilder.error(String.format("Provisioning a multi AZ cluster is only enabled for the following cloud platforms: %s.",
                        multiAzSupportedPlatforms.stream().map(CloudPlatform::name).sorted().collect(Collectors.joining(","))));
            }
            if (AZURE.equals(cloudPlatform) && !entitlementService.isAzureMultiAzEnabled(environmentResponse.getAccountId())) {
                validationBuilder.error(String.format("Provisioning a multi AZ cluster on Azure requires entitlement %s.",
                        Entitlement.CDP_CB_AZURE_MULTIAZ.name()));
            }
            if (AZURE.equals(cloudPlatform) && !clusterShape.isMultiAzEnabledByDefault()) {
                validationBuilder.error(String.format("Provisioning a multi AZ cluster on Azure is not supported for cluster shape %s.",
                        clusterShape.name()));
            }
            if (GCP.equals(cloudPlatform)) {
                validateMultiAzForGcp(environmentResponse.getAccountId(), clusterShape, validationBuilder);
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

    private StackV4Request getStackRequest(SdxClusterShape shape, StackV4Request internalStackV4Request,
            CloudPlatform cloudPlatform, String runtimeVersion, ImageSettingsV4Request imageSettingsV4Request) {
        StackV4Request stackV4Request = internalStackV4Request;
        if (!SdxClusterShape.CUSTOM.equals(shape)) {
            CDPConfigKey cdpConfigKey = new CDPConfigKey(cloudPlatform, shape, runtimeVersion);
            stackV4Request = cdpConfigService.getConfigForKey(cdpConfigKey);
            if (stackV4Request == null) {
                String message = "Can't find template for " + cdpConfigKey;
                LOGGER.error(message);
                throw new BadRequestException(message);
            }
            if (imageSettingsV4Request != null) {
                stackV4Request.setImage(imageSettingsV4Request);
            }
            ClouderaManagerV4Request clouderaManagerV4Request = Optional.ofNullable(internalStackV4Request)
                    .map(StackV4Request::getCluster)
                    .map(ClusterV4Request::getCm)
                    .orElse(null);
            if (clouderaManagerV4Request != null) {
                stackV4Request.getCluster().setCm(clouderaManagerV4Request);
            }
        }
        return stackV4Request;
    }

    private List<InstanceGroupV4Request> getInstanceGroupsByCDPConfig(SdxClusterShape shape, CloudPlatform cloudPlatform, String runtimeVersion) {
        CDPConfigKey cdpConfigKey = new CDPConfigKey(cloudPlatform, shape, runtimeVersion);
        StackV4Request stackV4Request = cdpConfigService.getConfigForKey(cdpConfigKey);
        if (stackV4Request == null) {
            return Collections.emptyList();
        }
        return stackV4Request.getInstanceGroups();
    }

    private String getRuntime(SdxClusterRequest sdxClusterRequest, StackV4Request stackV4Request, ImageV4Response imageV4Response) {
        return Optional.ofNullable(getRuntimeVersionFromImageResponse(imageV4Response))
                .or(() -> Optional.ofNullable(sdxClusterRequest.getRuntime()))
                .or(() -> extractRuntimeFromCdhProductVersion(stackV4Request))
                .orElse(cdpConfigService.getDefaultRuntime());
    }

    private Optional<String> extractRuntimeFromCdhProductVersion(StackV4Request stackV4Request) {
        return Optional.ofNullable(stackV4Request)
                .map(StackV4Request::getCluster)
                .map(ClusterV4Request::getCm)
                .map(ClouderaManagerV4Request::getProducts)
                .stream()
                .flatMap(List::stream)
                .filter(product -> "CDH".equalsIgnoreCase(product.getName()))
                .map(product -> StringUtils.substringBefore(product.getVersion(), "-"))
                .findFirst();
    }

    private String getOs(SdxClusterRequest sdxClusterRequest, StackV4Request internalStackV4Request, ImageV4Response imageV4Response) {
        return Optional.ofNullable(sdxClusterRequest.getOs())
                .or(() -> Optional.ofNullable(imageV4Response).map(ImageV4Response::getOs))
                .or(() -> Optional.ofNullable(internalStackV4Request).map(StackV4Request::getImage).map(ImageSettingsV4Request::getOs))
                .orElse(null);
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
            SdxCluster sdxCluster) {
        CloudStorageRequest cloudStorageRequest = cloudStorageManifester.initCloudStorageRequestFromExistingSdxCluster(
                stackV4Response.getCluster(), sdxCluster);
        stackV4Request.getCluster().setCloudStorage(cloudStorageRequest);
    }

    private void prepareImageForStack(StackV4Request stackV4Request, StackV4Response stackV4Response) {
        StackImageV4Response imageResponse = stackV4Response.getImage();
        if (imageResponse != null) {
            ImageSettingsV4Request imageSettingsV4Request = new ImageSettingsV4Request();
            imageSettingsV4Request.setOs(imageResponse.getOs());
            imageSettingsV4Request.setCatalog(imageResponse.getCatalogName());
            imageSettingsV4Request.setId(imageResponse.getId());
            stackV4Request.setImage(imageSettingsV4Request);
        }
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

    private void prepareDefaultSecurityConfigs(SdxClusterShape shape, StackV4Request stackV4Request, CloudPlatform cloudPlatform) {
        if (!SdxClusterShape.CUSTOM.equals(shape) && !List.of(CloudPlatform.MOCK, CloudPlatform.YARN).contains(cloudPlatform)) {
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
            if (!platformConfig.getRazSupportedPlatforms().contains(cloudPlatform)) {
                validationBuilder.error(String.format("Provisioning Ranger Raz is only valid for %s",
                        platformConfig.getRazSupportedPlatforms().stream()
                                .map(CloudPlatform::getDislayName)
                                .collect(Collectors.joining(", "))));
            } else if (!isRazSupported(runtime, cloudPlatform)) {
                validationBuilder.error(String.format("Provisioning Ranger Raz on %s is only valid for Cloudera Runtime version greater than or " +
                                "equal to %s and not %s", cloudPlatform.getDislayName(),
                        MIN_RUNTIME_VERSION_FOR_RAZ.get(cloudPlatform).getVersion(), runtime));
            } else if (cloudPlatform.equals(GCP) && !entitlementService.isRazForGcpEnabled(Crn.safeFromString(environment.getCreator()).getAccountId())) {
                validationBuilder.error("Provisioning Ranger Raz on GCP is not enabled for this account");
            }
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateRmsEnablement(String runtime, boolean razEnabled, boolean rmsEnabled, String cloudPlatformString, String accountId) {
        ValidationResultBuilder validationResultBuilder = new ValidationResultBuilder();
        CloudPlatform cloudPlatform = EnumUtils.getEnumIgnoreCase(CloudPlatform.class, cloudPlatformString);
        if (rmsEnabled) {
            if (!razEnabled) {
                validationResultBuilder.error("Ranger RMS cannot be deployed without Ranger RAZ");
            }
            if (AWS != cloudPlatform) {
                validationResultBuilder.error("Ranger RMS can be deployed only on AWS.");
            }
            if (!entitlementService.isRmsEnabledOnDatalake(accountId)) {
                validationResultBuilder.error("Provisioning Ranger RMS is not enabled for this account");
            }
            Comparator<Versioned> versionComparator = new VersionComparator();
            if (versionComparator.compare(() -> runtime, MIN_RUNTIME_VERSION_FOR_RMS) < 0) {
                validationResultBuilder.error(String.format("Provisioning Ranger RMS is only valid for Cloudera Runtime version greater then or equal to %s" +
                        " and not %s", MIN_RUNTIME_VERSION_FOR_RMS.getVersion(), runtime));
            }
        }
        ValidationResult validationResult = validationResultBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateJavaVersion(Integer javaVersion, String accountId) {
        if (javaVersion != null) {
            if (!virtualMachineConfiguration.getSupportedJavaVersions().contains(javaVersion)) {
                throw new BadRequestException(String.format("Java version %d is not supported.", javaVersion));
            }
        }
    }

    private void validateShape(SdxClusterShape shape, String runtime, DetailedEnvironmentResponse environment) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        if (SdxClusterShape.MICRO_DUTY.equals(shape)) {
            validateMicroDutyShape(runtime, environment, validationBuilder);
        } else if (SdxClusterShape.MEDIUM_DUTY_HA.equals(shape)) {
            validateMediumDutyShape(runtime, validationBuilder, environment.getAccountId());
        } else if (SdxClusterShape.ENTERPRISE.equals(shape)) {
            validateEnterpriseShape(runtime, validationBuilder);
        }
        ValidationResult validationResult = validationBuilder.build();
        if (validationResult.hasError()) {
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
    }

    private void validateEnterpriseShape(String runtime, ValidationResultBuilder validationBuilder) {
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, ENTERPRISE_DATALAKE_REQUIRED_VERSION)) {
            validationBuilder.error("Provisioning an Enterprise SDX shape is only valid for CM version greater than or equal to "
                    + ENTERPRISE_DATALAKE_REQUIRED_VERSION + " and not " + runtime);
        }
    }

    private void validateMicroDutyShape(String runtime, DetailedEnvironmentResponse environment, ValidationResultBuilder validationBuilder) {
        if (!entitlementService.microDutySdxEnabled(Crn.safeFromString(environment.getCreator()).getAccountId())) {
            validationBuilder.error(String.format("Provisioning a micro duty data lake cluster is not enabled for %s. " +
                    "Contact Cloudera support to enable CDP_MICRO_DUTY_SDX entitlement for the account.", environment.getCloudPlatform()));
        }
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, MICRO_DUTY_REQUIRED_VERSION)) {
            validationBuilder.error("Provisioning a Micro Duty SDX shape is only valid for CM version greater than or equal to "
                    + MICRO_DUTY_REQUIRED_VERSION + " and not " + runtime);
        }
    }

    private void validateMediumDutyShape(String runtime, ValidationResultBuilder validationBuilder, String accountId) {
        if (!isShapeVersionSupportedByMinimumRuntimeVersion(runtime, MEDIUM_DUTY_REQUIRED_VERSION)) {
            validationBuilder.error("Provisioning a Medium Duty SDX shape is only valid for CM version greater than or equal to "
                    + MEDIUM_DUTY_REQUIRED_VERSION + " and not " + runtime);
        }
        if (!isShapeVersionSupportedByMaximumRuntimeVersion(runtime, MEDIUM_DUTY_MAXIMUM_VERSION)
                && !entitlementService.isSdxRuntimeUpgradeEnabledOnMediumDuty(accountId)) {
            validationBuilder.error("Provisioning a Medium Duty SDX shape is only valid for 7.2.17 and below. If you want to provision a " +
                    runtime + " SDX, Please use the ENTERPRISE shape!");
        }
    }

    private boolean isShapeVersionSupportedByMinimumRuntimeVersion(String runtime, String shapeVersion) {
        // If runtime is empty, then SDX internal call was used, so we assume it's supported.
        if (StringUtils.isEmpty(runtime)) {
            return true;
        }
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(() -> runtime, () -> shapeVersion) > -1;
    }

    private boolean isShapeVersionSupportedByMaximumRuntimeVersion(String runtime, String shapeVersion) {
        // internal usage
        if (StringUtils.isEmpty(runtime)) {
            return true;
        }
        Comparator<Versioned> versionedComparator = new VersionComparator();
        return versionedComparator.compare(() -> runtime, () -> shapeVersion) < 1;
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
        return versionComparator.compare(() -> runtime, MIN_RUNTIME_VERSION_FOR_RAZ.get(cloudPlatform)) > -1;
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

    private void validateSdxResizeRequest(SdxCluster sdxCluster, String accountId, SdxClusterShape shape, boolean enableMultiAz) {
        if (!entitlementService.isDatalakeLightToMediumMigrationEnabled(accountId)) {
            throw new BadRequestException("Resizing of the data lake is not supported");
        }
        if (sdxCluster.getClusterShape() == shape && (!enableMultiAz || sdxCluster.isEnableMultiAz())) {
            throw new BadRequestException("SDX cluster is already of requested shape and not resizing to multi AZ from single AZ");
        }

        sdxClusterRepository.findByAccountIdAndEnvCrnAndDeletedIsNullAndDetachedIsTrue(accountId, sdxCluster.getEnvCrn())
                .ifPresent(existedSdx -> {
                    throw new BadRequestException("SDX which is detached already exists for the environment. SDX name: " + existedSdx.getClusterName());
                });
        Boolean datalakeIsInBackupProgress = sdxBackupRestoreService.isDatalakeInBackupProgress(sdxCluster.getClusterName(),
                ThreadBasedUserCrnProvider.getUserCrn());
        if (datalakeIsInBackupProgress) {
            throw new BadRequestException("SDX cluster is in the process of backup. Resize can not get started.");
        }
        Boolean datalakeIsInRestoreProgress = sdxBackupRestoreService.isDatalakeInRestoreProgress(sdxCluster.getClusterName(),
                ThreadBasedUserCrnProvider.getUserCrn());
        if (datalakeIsInRestoreProgress) {
            throw new BadRequestException("SDX cluster is in the process of restore. Resize can not get started.");
        }
    }

    private void validateImageRequest(SdxClusterRequest sdxClusterRequest, ImageSettingsV4Request imageSettingsV4Request) {
        boolean hasImageId = imageSettingsV4Request != null && StringUtils.isNotBlank(imageSettingsV4Request.getId());
        if (hasImageId && StringUtils.isNotBlank(imageSettingsV4Request.getOs())) {
            throw new BadRequestException("Image request can not have both image id and os parameters set.");
        }
    }

    @VisibleForTesting
    void validateRuntimeAndImage(SdxClusterRequest clusterRequest, DetailedEnvironmentResponse environment,
            ImageSettingsV4Request imageSettingsV4Request, ImageV4Response imageV4Response) {
        ValidationResultBuilder validationBuilder = new ValidationResultBuilder();
        CloudPlatform cloudPlatform = EnumUtils.getEnumIgnoreCase(CloudPlatform.class, environment.getCloudPlatform());

        if (imageV4Response != null) {
            if (StringUtils.isNotBlank(imageV4Response.getVersion()) && StringUtils.isNotBlank(clusterRequest.getRuntime())
                    && !Objects.equals(clusterRequest.getRuntime(), imageV4Response.getVersion())) {
                validationBuilder.error("SDX cluster request must not specify both runtime version and image at the same time because image " +
                        "decides runtime version.");
            }
            if (Architecture.fromStringWithFallback(imageV4Response.getArchitecture()) != Architecture.X86_64) {
                validationBuilder.error("SDX cluster request image must have x86_64 architecture.");
            }
        } else if (isImageSpecified(imageSettingsV4Request) && StringUtils.isBlank(clusterRequest.getRuntime())) {
            if (cloudPlatform.equals(MOCK)) {
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

    private boolean isImageSpecified(ImageSettingsV4Request imageSettingsV4Request) {
        return imageSettingsV4Request != null && !StringUtils.isBlank(imageSettingsV4Request.getId());
    }

    @VisibleForTesting
    void validateInternalSdxRequest(StackV4Request stackv4Request, SdxClusterRequest sdxClusterRequest) {
        ValidationResultBuilder validationResultBuilder = ValidationResult.builder();
        if (stackv4Request != null) {
            if (stackv4Request.getCluster() == null) {
                validationResultBuilder.error("Cluster cannot be null.");
            }
            if (CollectionUtils.isNotEmpty(sdxClusterRequest.getCustomInstanceGroups())) {
                validationResultBuilder.error("Custom instance group is not accepted on SDX Internal API.");
            }
        } else if (SdxClusterShape.CUSTOM.equals(sdxClusterRequest.getClusterShape())) {
            validationResultBuilder.error("CUSTOM cluster shape requires stack request.");
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

    private String createCrn(@Nonnull String accountId) {
        return regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.VM_DATALAKE, accountId);
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

    private boolean isGovCloudEnvironment(DetailedEnvironmentResponse environmentResponse) {
        return environmentResponse.getCredential() != null &&
                environmentResponse.getCredential().getGovCloud() != null &&
                environmentResponse.getCredential().getGovCloud().booleanValue();
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
            DetailedEnvironmentResponse envResp = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> environmentClientService.getByCrn(sdxCluster.getEnvCrn()));
            return PayloadContext.create(sdxCluster.getCrn(), envResp.getCloudPlatform());
        } catch (NotFoundException ignored) {
            LOGGER.info("exception happened {}", ignored);
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

    public FlowIdentifier rotateSaltPassword(SdxCluster sdxCluster, RotateSaltPasswordReason reason) {
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                initiatorUserCrn -> stackV4Endpoint.rotateSaltPasswordInternal(WORKSPACE_ID_DEFAULT, sdxCluster.getCrn(),
                        new RotateSaltPasswordRequest(reason), initiatorUserCrn)
        );
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        return sdxReactorFlowManager.triggerSaltPasswordRotationTracker(sdxCluster);
    }

    public FlowIdentifier modifyProxyConfig(SdxCluster sdxCluster, String previousProxyConfigCrn) {
        FlowIdentifier flowIdentifier = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                initiatorUserCrn -> stackV4Endpoint.modifyProxyConfigInternal(WORKSPACE_ID_DEFAULT, sdxCluster.getCrn(),
                        previousProxyConfigCrn, initiatorUserCrn)
        );
        cloudbreakFlowService.saveLastCloudbreakFlowChainId(sdxCluster, flowIdentifier);
        return sdxReactorFlowManager.triggerModifyProxyConfigTracker(sdxCluster);
    }

    public void updateDatabaseEngineVersion(SdxCluster sdxCluster, String databaseEngineVersion) {
        updateDatabaseEngineVersion(sdxCluster.getCrn(), databaseEngineVersion);
        sdxCluster.getSdxDatabase().setDatabaseEngineVersion(databaseEngineVersion);
    }

    public void updateDatabaseEngineVersion(String crn, String databaseEngineVersion) {
        Optional<Long> databaseId = sdxClusterRepository.findDatabaseIdByCrn(crn);
        databaseId.ifPresentOrElse(id -> sdxDatabaseRepository.updateDatabaseEngineVersion(id, databaseEngineVersion), () -> {
            throw notFoundException("SdxCluster with", crn + " crn");
        });
        LOGGER.info("Updated database engine version for [{}] with [{}]", crn, databaseEngineVersion);
    }

    public FlowIdentifier updateSalt(SdxCluster sdxCluster) {
        SdxStatusEntity sdxStatus = sdxStatusService.getActualStatusForSdx(sdxCluster);
        DatalakeStatusEnum status = sdxStatus.getStatus();
        if (status.isStopState() || status.isDeleteInProgressOrCompleted()) {
            String message = String.format("SaltStack update cannot be initiated as datalake '%s' is currently in '%s' state.",
                    sdxCluster.getName(), status);
            LOGGER.info(message);
            throw new BadRequestException(message);
        } else {
            return sdxReactorFlowManager.triggerSaltUpdate(sdxCluster);
        }
    }

    public void validateRdsSslCertRotation(String crn) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String operationNameToValidate = "RDS SSL certificate rotation";
        Runnable endpointCall = () -> stackV4Endpoint.validateRotateRdsCertificateByCrnInternal(WORKSPACE_ID_DEFAULT, crn, userCrn);
        validateOnCoreInternalApiWithExceptionHandling(endpointCall, operationNameToValidate);
    }

    public void validateDefaultJavaVersionUpdate(String crn, SetDefaultJavaVersionRequest request) {
        String operationNameToValidate = "default Java version update";
        Runnable endpointCall = () -> stackV4Endpoint.validateDefaultJavaVersionUpdateByCrnInternal(WORKSPACE_ID_DEFAULT, crn, request);
        validateOnCoreInternalApiWithExceptionHandling(endpointCall, operationNameToValidate);
    }

    private void validateOnCoreInternalApiWithExceptionHandling(Runnable endpointCall, String operationNameToValidate) {
        LOGGER.info("Calling Cloudbreak to validate {} is triggerable", operationNameToValidate);
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    endpointCall);
        } catch (jakarta.ws.rs.BadRequestException e) {
            String msg = String.format("Validation failed %s is not triggerable", operationNameToValidate);
            LOGGER.info(msg, e);
            String message = Optional.ofNullable(e.getResponse())
                    .map(resp -> resp.hasEntity() ? resp.readEntity(ExceptionResponse.class) : new ExceptionResponse(msg))
                    .map(ExceptionResponse::getMessage)
                    .orElse(msg);
            throw new BadRequestException(message, e);
        } catch (jakarta.ws.rs.WebApplicationException wae) {
            String msg = String.format("Failed to validate %s for SDX", operationNameToValidate);
            LOGGER.warn(msg, wae);
            throw new IllegalStateException(msg, wae);
        }
    }

    private NetworkV4Request createNetworkRequestFromCurrentDatalake(NetworkV4Response networkFromCurrentDatalake) {
        LOGGER.info("Datalake resize will use network details from the original datalake.");

        NetworkV4Request networkRequest = new NetworkV4Request();

        networkRequest.setSubnetCIDR(networkFromCurrentDatalake.getSubnetCIDR());
        networkRequest.setCloudPlatform(networkFromCurrentDatalake.getCloudPlatform());
        if (networkFromCurrentDatalake.getAws() != null) {
            networkRequest.setAws(networkFromCurrentDatalake.getAws());
        }
        if (networkFromCurrentDatalake.getAzure() != null) {
            networkRequest.setAzure(networkFromCurrentDatalake.getAzure());
        }
        if (networkFromCurrentDatalake.getGcp() != null) {
            networkRequest.setGcp(networkFromCurrentDatalake.getGcp());
        }
        if (networkFromCurrentDatalake.getMock() != null) {
            networkRequest.setMock(networkFromCurrentDatalake.getMock());
        }

        return networkRequest;
    }

    private void validateMultiAzForGcp(String accountId, SdxClusterShape clusterShape, ValidationResultBuilder validationBuilder) {
        if (!entitlementService.isGcpMultiAzEnabled(accountId)) {
            validationBuilder.error(String.format("Provisioning a multi AZ cluster on GCP requires entitlement %s.",
                    Entitlement.CDP_CB_GCP_MULTIAZ.name()));
        }

        if (!clusterShape.isMultiAzEnabledByDefault()) {
            validationBuilder.error(String.format("Provisioning a multi AZ cluster on GCP is not supported for cluster shape %s.",
                    clusterShape.name()));
        }
    }
}