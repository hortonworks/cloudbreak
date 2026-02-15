package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.CALLER_ID_NOT_FOUND;
import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.CDP_CALLER_ID_HEADER;
import static com.sequenceiq.cloudbreak.common.request.CreatorClientConstants.USER_AGENT_HEADER;
import static com.sequenceiq.cloudbreak.common.request.HeaderValueProvider.getHeaderOrItsFallbackValueOrDefault;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.RecipeV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StackResponseEntries;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SecurityV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.image.ImageSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.image.StackImageV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.converter.NetworkV4ResponseToNetworkV4RequestConverter;
import com.sequenceiq.datalake.service.sdx.database.DatabaseParameterInitUtil;
import com.sequenceiq.datalake.service.sdx.dr.SdxBackupRestoreService;
import com.sequenceiq.datalake.service.validation.resize.SdxResizeValidator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.sdx.api.model.SdxClusterResizeRequest;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseComputeStorageRequest;

@Service
public class SdxResizeService {

    public static final String INSTANCE_TYPE = "instancetype";

    public static final String STORAGE = "storage";

    public static final String PREVIOUS_DATABASE_CRN = "previousDatabaseCrn";

    public static final String PREVIOUS_CLUSTER_SHAPE = "previousClusterShape";

    public static final String DATABASE_SSL_ENABLED = "databaseSslEnabled";

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxResizeService.class);

    private static final String SDX_CLUSTER = "SDX cluster";

    @Value("${info.app.version}")
    private String sdxClusterServiceVersion;

    @Inject
    private Clock clock;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private MultiAzDecorator multiAzDecorator;

    @Inject
    private SdxResizeValidator sdxResizeValidator;

    @Inject
    private StackRequestHandler stackRequestHandler;

    @Inject
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Inject
    private CloudStorageManifester cloudStorageManifester;

    @Inject
    private ShapeValidator shapeValidator;

    @Inject
    private NetworkV4ResponseToNetworkV4RequestConverter networkV4ResponseToNetworkV4RequestConverter;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CDPConfigService cdpConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private SdxBackupRestoreService sdxBackupRestoreService;

    @Inject
    private SdxRecommendationService sdxRecommendationService;

    @Inject
    private SdxInstanceService sdxInstanceService;

    @Inject
    private RangerRazService rangerRazService;

    @Inject
    private RangerRmsService rangerRmsService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private EnvironmentService environmentService;

    @Inject
    private AccountIdService accountIdService;

    public Pair<SdxCluster, FlowIdentifier> resizeSdx(String userCrn, String clusterName, SdxClusterResizeRequest sdxClusterResizeRequest) {
        LOGGER.info("Re-sizing SDX cluster with name {}", clusterName);
        String accountIdFromCrn = accountIdService.getAccountIdFromUserCrn(userCrn);
        String environmentName = sdxClusterResizeRequest.getEnvironment();
        SdxClusterShape shape = sdxClusterResizeRequest.getClusterShape();

        if (sdxClusterResizeRequest.isValidationOnly() && sdxClusterResizeRequest.isSkipValidation()) {
            throw new BadRequestException("The Validation Only flag cannot be used with the SkipValidation flag");
        }

        SdxCluster sdxCluster = sdxClusterRepository.findByAccountIdAndClusterNameAndDeletedIsNullAndDetachedIsFalse(accountIdFromCrn, clusterName)
                .orElseThrow(() -> notFound(SDX_CLUSTER, clusterName).get());

        MDCBuilder.buildMdcContext(sdxCluster);

        validateSdxResizeRequest(sdxCluster, accountIdFromCrn, shape, sdxClusterResizeRequest.isEnableMultiAz());
        StackV4Response stackV4Response = stackService.getDetail(clusterName,
                Set.of(StackResponseEntries.HARDWARE_INFO.getEntryName(), StackResponseEntries.EVENTS.getEntryName()), accountIdFromCrn);

        DetailedEnvironmentResponse environment = environmentService.validateAndGetEnvironment(environmentName);
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform());
        sdxResizeValidator.validateDatabaseTypeForResize(sdxCluster.getSdxDatabase(), cloudPlatform);

        SdxCluster newSdxCluster = validateAndCreateNewSdxClusterForResize(sdxCluster, shape, sdxCluster.isEnableMultiAz()
                || sdxClusterResizeRequest.isEnableMultiAz(), clusterName, userCrn, environment);
        newSdxCluster.setTags(sdxCluster.getTags());
        newSdxCluster.setCrn(sdxCluster.getCrn());
        newSdxCluster.setArchitecture(sdxCluster.getArchitecture());
        if (!StringUtils.isBlank(sdxCluster.getCloudStorageBaseLocation())) {
            newSdxCluster.setCloudStorageBaseLocation(sdxCluster.getCloudStorageBaseLocation());
            newSdxCluster.setCloudStorageFileSystemType(sdxCluster.getCloudStorageFileSystemType());
        } else if (!CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform.name()) &&
                !GCP.equalsIgnoreCase(cloudPlatform.name()) &&
                !MOCK.equalsIgnoreCase(cloudPlatform.name())) {
            throw new BadRequestException("Cloud storage parameter is required.");
        }

        newSdxCluster.setSdxDatabase(DatabaseParameterInitUtil.setupDatabaseInitParams(sdxCluster.getDatabaseAvailabilityType(),
                sdxCluster.getDatabaseEngineVersion(), Optional.ofNullable(sdxCluster.getSdxDatabase()).map(SdxDatabase::getAttributes).orElse(null)));
        StackV4Request stackRequest = stackRequestHandler.getStackRequest(shape, null, cloudPlatform,
                sdxCluster.getRuntime(), null, Optional.ofNullable(sdxCluster.getArchitecture()).orElse(Architecture.X86_64));
        stackRequestHandler.setStackRequestParams(stackRequest, stackV4Response.getJavaVersion(), sdxCluster.isRangerRazEnabled(),
                sdxCluster.isRangerRmsEnabled(), stackV4Response.getCluster().getEncryptionProfileCrn());
        setSecurityRequest(sdxCluster, stackRequest);
        setRecipesFromStackV4ResponseToStackV4Request(stackV4Response, stackRequest);
        CustomDomainSettingsV4Request customDomainSettingsV4Request = new CustomDomainSettingsV4Request();
        String azSuffix = (shape == sdxCluster.getClusterShape()) ? "-az" : "";
        customDomainSettingsV4Request.setHostname(sdxCluster.getClusterName() + shape.getResizeSuffix() + azSuffix);
        stackRequest.setCustomDomain(customDomainSettingsV4Request);

        prepareCloudStorageForStack(stackRequest, stackV4Response, newSdxCluster);
        securityConfigService.prepareDefaultSecurityConfigs(null, stackRequest, cloudPlatform);
        prepareImageForStack(stackRequest, stackV4Response);

        stackRequest.setResourceCrn(newSdxCluster.getCrn());
        List<InstanceGroupV4Request> originalInstanceGroups = getInstanceGroupsByCDPConfig(sdxCluster.getClusterShape(), cloudPlatform, sdxCluster.getRuntime(),
                sdxCluster.getArchitecture());
        sdxInstanceService.overrideDefaultInstanceType(stackRequest, sdxClusterResizeRequest.getCustomInstanceGroups(), originalInstanceGroups,
                stackV4Response.getInstanceGroups(), sdxCluster.getClusterShape());
        sdxInstanceService.overrideDefaultInstanceStorage(stackRequest, sdxClusterResizeRequest.getCustomInstanceGroupDiskSize(),
                stackV4Response.getInstanceGroups(), sdxCluster.getClusterShape());
        overrideDefaultDatabaseProperties(newSdxCluster.getSdxDatabase(), sdxClusterResizeRequest.getCustomSdxDatabaseComputeStorage(),
                sdxCluster.getSdxDatabase().getDatabaseCrn(), sdxCluster.getClusterShape(), stackV4Response.getCluster().isDbSSLEnabled());
        stackRequest.setNetwork(networkV4ResponseToNetworkV4RequestConverter.convert(stackV4Response.getNetwork()));

        if (newSdxCluster.isEnableMultiAz()) {
            multiAzDecorator.decorateRequestWithMultiAz(stackRequest, stackV4Response, environment, sdxCluster.getClusterShape(), sdxCluster.isEnableMultiAz());
        }

        newSdxCluster.setStackRequest(stackRequest);
        sdxRecommendationService.validateVmTypeOverride(environment, newSdxCluster);
        FlowIdentifier flowIdentifier = sdxReactorFlowManager.triggerSdxResize(sdxCluster.getId(), newSdxCluster,
                new DatalakeDrSkipOptions(sdxClusterResizeRequest.isSkipValidation(), sdxClusterResizeRequest.isSkipAtlasMetadata(),
                        sdxClusterResizeRequest.isSkipRangerAudits(), sdxClusterResizeRequest.isSkipRangerMetadata()),
                sdxClusterResizeRequest.isValidationOnly());
        return Pair.of(sdxCluster, flowIdentifier);
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
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (sdxBackupRestoreService.isDatalakeInBackupProgress(sdxCluster.getClusterName(), userCrn)) {
            throw new BadRequestException("SDX cluster is in the process of backup. Resize can not get started.");
        }
        if (sdxBackupRestoreService.isDatalakeInRestoreProgress(sdxCluster.getClusterName(), userCrn)) {
            throw new BadRequestException("SDX cluster is in the process of restore. Resize can not get started.");
        }
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

    private SdxCluster validateAndCreateNewSdxClusterForResize(SdxCluster sdxCluster, SdxClusterShape shape, boolean enableMultiAz,
            String clusterName, String userCrn, DetailedEnvironmentResponse environmentResponse) {
        shapeValidator.validateShape(shape, sdxCluster.getRuntime(), environmentResponse);
        rangerRazService.validateRazEnablement(sdxCluster.getRuntime(), sdxCluster.isRangerRazEnabled(), environmentResponse);
        rangerRmsService.validateRmsEnablement(sdxCluster.getRuntime(), sdxCluster.isRangerRazEnabled(), sdxCluster.isRangerRmsEnabled(),
                environmentResponse.getCloudPlatform(), environmentResponse.getAccountId());
        multiAzDecorator.validateMultiAz(enableMultiAz, environmentResponse, shape, true);
        SdxCluster newSdxCluster = new SdxCluster();
        newSdxCluster.setCrn(regionAwareCrnGenerator.generateCrnStringWithUuid(CrnResourceDescriptor.VM_DATALAKE,
                accountIdService.getAccountIdFromUserCrn(userCrn)));
        newSdxCluster.setClusterName(clusterName);
        newSdxCluster.setAccountId(accountIdService.getAccountIdFromUserCrn(userCrn));
        newSdxCluster.setClusterShape(shape);
        newSdxCluster.setSeLinux(sdxCluster.getSeLinux());
        newSdxCluster.setNotificationState(sdxCluster.getNotificationState());
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

    private void prepareCloudStorageForStack(StackV4Request stackV4Request, StackV4Response stackV4Response,
            SdxCluster sdxCluster) {
        CloudStorageRequest cloudStorageRequest = cloudStorageManifester.initCloudStorageRequestFromExistingSdxCluster(
                stackV4Response.getCluster(), sdxCluster);
        stackV4Request.getCluster().setCloudStorage(cloudStorageRequest);
    }

    private void setSecurityRequest(SdxCluster sdxCluster, StackV4Request stackRequest) {
        SecurityV4Request securityV4Request = new SecurityV4Request();
        SeLinux seLinux = sdxCluster.getSeLinux();
        securityV4Request.setSeLinux(seLinux == null ? SeLinux.PERMISSIVE.name() : sdxCluster.getSeLinux().name());
        stackRequest.setSecurity(securityV4Request);
    }

    private List<InstanceGroupV4Request> getInstanceGroupsByCDPConfig(SdxClusterShape shape, CloudPlatform cloudPlatform, String runtimeVersion,
            Architecture architecture) {
        CDPConfigKey cdpConfigKey = new CDPConfigKey(cloudPlatform, shape, runtimeVersion, architecture);
        StackV4Request stackV4Request = cdpConfigService.getConfigForKey(cdpConfigKey);
        if (stackV4Request == null) {
            return Collections.emptyList();
        }
        return stackV4Request.getInstanceGroups();
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
}
