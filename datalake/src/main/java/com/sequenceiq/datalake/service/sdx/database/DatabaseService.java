package com.sequenceiq.datalake.service.sdx.database;

import static com.sequenceiq.common.model.AzureDatabaseType.FLEXIBLE_SERVER;
import static com.sequenceiq.common.model.AzureDatabaseType.SINGLE_SERVER;
import static com.sequenceiq.datalake.service.TagUtil.getTags;
import static com.sequenceiq.datalake.service.sdx.SdxService.DATABASE_SSL_ENABLED;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.util.VersionComparator;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AzureDatabaseType;
import com.sequenceiq.common.model.DatabaseCapabilityType;
import com.sequenceiq.common.model.DatabaseType;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.converter.DatabaseServerConverter;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.events.EventSenderService;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.repository.SdxDatabaseRepository;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxDatabaseOperation;
import com.sequenceiq.datalake.service.sdx.SdxNotificationService;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.EnvironmentPlatformResourceEndpoint;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDatabaseCapabilitiesResponse;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslConfigV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.SslMode;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.operation.OperationV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class DatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

    private static final String SSL_ENFORCEMENT_MIN_RUNTIME_VERSION = "7.2.2";

    private static final String INSTANCE_TYPE = "instancetype";

    private static final String STORAGE = "storage";

    private static final String PREVIOUS_DATABASE_CRN = "previousDatabaseCrn";

    private static final String PREVIOUS_CLUSTER_SHAPE = "previousClusterShape";

    private final Comparator<Versioned> versionComparator = new VersionComparator();

    @Value("${sdx.db.operation.sleeptime_sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.db.operation.duration_min:80}")
    private int durationInMinutes;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxDatabaseRepository sdxDatabaseRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private SdxService sdxService;

    @Inject
    private DatabaseServerConverter databaseServerConverter;

    @Inject
    private Map<DatabaseConfigKey, DatabaseConfig> dbConfigs;

    @Inject
    private Map<CloudPlatform, DatabaseServerParameterSetter> databaseServerParameterSetterMap;

    @Inject
    private PlatformConfig platformConfig;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    @Inject
    private OperationV4Endpoint operationV4Endpoint;

    @Inject
    private EnvironmentPlatformResourceEndpoint environmentPlatformResourceEndpoint;

    @Inject
    private AzureDatabaseAttributesService azureDatabaseAttributesService;

    @Inject
    private SdxNotificationService sdxNotificationService;

    @Inject
    private EventSenderService eventSenderService;

    public DatabaseServerStatusV4Response create(SdxCluster sdxCluster, DetailedEnvironmentResponse env) {
        LOGGER.info("Create databaseServer in environment {} for SDX {}", env.getName(), sdxCluster.getClusterName());
        String dbResourceCrn;
        DatabaseServerStatusV4Response serverStatusV4Response = null;
        if (dbHasBeenCreatedPreviously(sdxCluster)) {
            dbResourceCrn = sdxCluster.getDatabaseCrn();
        } else {
            try {
                String initiatorUserCrn = ThreadBasedUserCrnProvider.getUserCrn();
                if (sdxStatusService.getActualStatusForSdx(sdxCluster).getStatus().isDeleteInProgressOrCompleted()) {
                    throw new CloudbreakServiceException("Datalake deletion in progress! Do not provision database, create flow cancelled");
                }
                serverStatusV4Response =
                ThreadBasedUserCrnProvider.doAsInternalActor(
                        () -> databaseServerV4Endpoint.createInternal(getDatabaseRequest(sdxCluster, env, initiatorUserCrn), initiatorUserCrn));
                dbResourceCrn = serverStatusV4Response.getResourceCrn();
                DatabaseParameterFallbackUtil.setDatabaseCrn(sdxCluster, dbResourceCrn);
                sdxDatabaseRepository.save(sdxCluster.getSdxDatabase());
                sdxClusterRepository.save(sdxCluster);
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_CREATION_IN_PROGRESS,
                        "External database creation in progress", sdxCluster);
            } catch (BadRequestException badRequestException) {
                LOGGER.error("Redbeams create request failed, bad request", badRequestException);
                throw badRequestException;
            }
        }
        if (serverStatusV4Response != null && serverStatusV4Response.getStatus().equals(Status.STOPPED)) {
            throw new CloudbreakServiceException(String.format("Database already in %s state. Provisioning new database failed",
                    serverStatusV4Response.getStatus()));
        }
        return waitAndGetDatabase(sdxCluster, dbResourceCrn, SdxDatabaseOperation.CREATION, true);
    }

    private boolean dbHasBeenCreatedPreviously(SdxCluster sdxCluster) {
        return StringUtils.isNotEmpty(sdxCluster.getDatabaseCrn());
    }

    public void terminate(SdxCluster sdxCluster, boolean forced) {
        LOGGER.info("Terminating databaseServer of SDX {}", sdxCluster.getClusterName());
        try {
            DatabaseServerV4Response resp = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> databaseServerV4Endpoint.deleteByCrn(sdxCluster.getDatabaseCrn(), forced));
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
                    "External database deletion in progress", sdxCluster);
            waitAndGetDatabase(sdxCluster, resp.getCrn(), SdxDatabaseOperation.DELETION, false);
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server is deleted on redbeams side {}", sdxCluster.getDatabaseCrn());
        }
    }

    public void start(SdxCluster sdxCluster) {
        LOGGER.info("Starting databaseServer of SDX {}", sdxCluster.getClusterName());
        String databaseCrn = sdxCluster.getDatabaseCrn();
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> databaseServerV4Endpoint.start(databaseCrn));
            waitAndGetDatabase(sdxCluster, databaseCrn, SdxDatabaseOperation.START, false);
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    public void stop(SdxCluster sdxCluster) {
        LOGGER.info("Stopping databaseServer of SDX {}", sdxCluster.getClusterName());
        String databaseCrn = sdxCluster.getDatabaseCrn();
        try {
            ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> databaseServerV4Endpoint.stop(databaseCrn));
            waitAndGetDatabase(sdxCluster, databaseCrn, SdxDatabaseOperation.STOP, false);
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(SdxCluster sdxCluster, DetailedEnvironmentResponse env, String initiatorUserCrn) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        String environmentCrn = env.getCrn();
        req.setEnvironmentCrn(environmentCrn);
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(env.getCloudPlatform().toUpperCase(Locale.ROOT));
        req.setDatabaseServer(getDatabaseServerRequest(cloudPlatform, sdxCluster, env, initiatorUserCrn));
        req.setTags(getTags(sdxCluster.getTags()));
        req.setClusterCrn(sdxCluster.getCrn());

        Map<String, Object> attributes = sdxCluster.getSdxDatabase().getAttributes() != null ?
                sdxCluster.getSdxDatabase().getAttributes().getMap() :
                new HashMap<>();
        boolean previousDBSSLEnabled = true;
        if (attributes.containsKey(DATABASE_SSL_ENABLED) && attributes.get(DATABASE_SSL_ENABLED) instanceof Boolean) {
            previousDBSSLEnabled = (boolean) attributes.get(DATABASE_SSL_ENABLED);
        }

        String runtime = sdxCluster.getRuntime();
        if (previousDBSSLEnabled && platformConfig.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform) && isSslEnforcementSupportedForRuntime(runtime)) {
            LOGGER.info("Applying external DB SSL enforcement for cloud platform {} and runtime version {}", cloudPlatform, runtime);
            SslConfigV4Request sslConfigV4Request = new SslConfigV4Request();
            sslConfigV4Request.setSslMode(SslMode.ENABLED);
            req.setSslConfig(sslConfigV4Request);
        } else {
            LOGGER.info("Skipping external DB SSL enforcement for cloud platform {}, runtime version {}, and previous DB SSL enabled = {}",
                    cloudPlatform, runtime, previousDBSSLEnabled);
        }

        return req;
    }

    private boolean isSslEnforcementSupportedForRuntime(String runtime) {
        if (StringUtils.isBlank(runtime)) {
            // This may happen for custom data lakes
            LOGGER.info("Runtime is NOT specified, external DB SSL enforcement is permitted");
            return true;
        }
        boolean permitted = isVersionNewerThanOrEqualTo(() -> runtime, () -> SSL_ENFORCEMENT_MIN_RUNTIME_VERSION);
        LOGGER.info("External DB SSL enforcement {} permitted for runtime version {}", permitted ? "is" : "is NOT", runtime);
        return permitted;
    }

    private boolean isVersionNewerThanOrEqualTo(Versioned currentVersion, Versioned baseVersion) {
        LOGGER.info("Comparing current version {} with base version {}", currentVersion.getVersion(), baseVersion.getVersion());
        return versionComparator.compare(currentVersion, baseVersion) > -1;
    }

    @VisibleForTesting
    DatabaseServerV4StackRequest getDatabaseServerRequest(CloudPlatform cloudPlatform, SdxCluster sdxCluster,
            DetailedEnvironmentResponse env, String initiatorUserCrn) {
        DatabaseServerParameterSetter databaseServerParameterSetter = databaseServerParameterSetterMap.get(cloudPlatform);
        DatabaseType databaseType = databaseServerParameterSetter.getDatabaseType(sdxCluster.getSdxDatabase()).orElse(null);
        DatabaseConfig databaseConfig = dbConfigs.get(new DatabaseConfigKey(cloudPlatform, sdxCluster.getClusterShape(),
                databaseType));
        if (databaseConfig == null) {
            throw new BadRequestException("Database config for cloud platform " + cloudPlatform + ", cluster shape "
                    + sdxCluster.getClusterShape() + " not found");
        }
        SdxDatabase sdxDatabase = sdxCluster.getSdxDatabase();
        Map<String, Object> attributes = sdxDatabase.getAttributes() != null ? sdxDatabase.getAttributes().getMap() : new HashMap<>();
        String instanceType;
        Long storageSize;
        // Cascade instanceType and storageSize of the previous database if they are different of the default ones.
        Optional<DatabaseServerV4Response> previousDatabaseOp = getPreviousDatabaseIfPropertiesWereModified(attributes, cloudPlatform, sdxCluster);
        if (previousDatabaseOp.isPresent()) {
            DatabaseServerV4Response previousDatabase = previousDatabaseOp.get();
            instanceType = previousDatabase.getInstanceType();
            storageSize = previousDatabase.getStorageSize();
        } else {
            DatabaseCapabilityType databaseCapabilityType = CloudPlatform.AZURE.equals(cloudPlatform) ?
                    getAzureDatabaseCapability(databaseType) : DatabaseCapabilityType.DEFAULT;
            PlatformDatabaseCapabilitiesResponse databaseCapabilities = getDatabaseCapabilities(env, initiatorUserCrn, databaseCapabilityType,
                    sdxCluster.getArchitecture());
            instanceType = databaseCapabilities.getRegionDefaultInstances().get(env.getLocation().getName());
            if (instanceType == null && Architecture.ARM64.equals(sdxCluster.getArchitecture())) {
                sendArmDatabaseNotAvailableNotification(sdxCluster, initiatorUserCrn, env.getLocation().getName());
                databaseCapabilities = getDatabaseCapabilities(env, initiatorUserCrn, databaseCapabilityType, Architecture.X86_64);
                instanceType = databaseCapabilities.getRegionDefaultInstances().get(env.getLocation().getName());
            }
            storageSize = databaseConfig.getVolumeSize();
        }

        DatabaseServerV4StackRequest req = new DatabaseServerV4StackRequest();
        req.setInstanceType(attributes.containsKey(INSTANCE_TYPE) ? attributes.get(INSTANCE_TYPE).toString() : instanceType);
        req.setDatabaseVendor(databaseConfig.getVendor());
        req.setStorageSize(attributes.containsKey(STORAGE) ? Long.parseLong(attributes.get(STORAGE).toString()) : storageSize);
        databaseServerParameterSetter.setParameters(req, sdxCluster, env, initiatorUserCrn);
        databaseServerParameterSetter.validate(req, sdxCluster, env, initiatorUserCrn);

        LOGGER.info("Database requested parameters {}", req);

        return req;
    }

    private void sendArmDatabaseNotAvailableNotification(SdxCluster sdxCluster, String initiatorUserCrn, String region) {
        LOGGER.info("Arm64 database is not available in current region. Defaulting to x86.");
        ThreadBasedUserCrnProvider.doAs(initiatorUserCrn,
                () -> {
                    sdxNotificationService.send(ResourceEvent.DATABASE_ARM_NOT_AVAILABLE, List.of(region), sdxCluster, initiatorUserCrn);
                    eventSenderService.sendEventAndNotification(sdxCluster, ResourceEvent.DATABASE_ARM_NOT_AVAILABLE, List.of(region));
                });
    }

    private DatabaseCapabilityType getAzureDatabaseCapability(DatabaseType databaseType) {
        return FLEXIBLE_SERVER.equals(databaseType) ? DatabaseCapabilityType.AZURE_FLEXIBLE : DatabaseCapabilityType.AZURE_SINGLE_SERVER;
    }

    private PlatformDatabaseCapabilitiesResponse getDatabaseCapabilities(DetailedEnvironmentResponse env, String initiatorUserCrn,
            DatabaseCapabilityType capabilityType, Architecture architecture) {
        return ThreadBasedUserCrnProvider.doAs(initiatorUserCrn, () -> environmentPlatformResourceEndpoint.getDatabaseCapabilities(
                env.getCrn(),
                env.getLocation().getName(),
                env.getCloudPlatform(),
                null,
                capabilityType,
                architecture == null ? null : architecture.getName()));
    }

    private Optional<DatabaseServerV4Response> getPreviousDatabaseIfPropertiesWereModified(Map<String, Object> attributes, CloudPlatform cloudPlatform,
            SdxCluster sdxCluster) {
        DatabaseServerParameterSetter databaseServerParameterSetter = databaseServerParameterSetterMap.get(cloudPlatform);
        if (attributes.containsKey(PREVIOUS_DATABASE_CRN) && attributes.containsKey(PREVIOUS_CLUSTER_SHAPE)) {
            DatabaseServerV4Response previousDatabase = getDatabaseServerV4Response(attributes.get(PREVIOUS_DATABASE_CRN).toString());
            DatabaseConfig previousDatabaseConfig = dbConfigs.get(new DatabaseConfigKey(cloudPlatform,
                    SdxClusterShape.valueOf(attributes.get(PREVIOUS_CLUSTER_SHAPE).toString()),
                    databaseServerParameterSetter.getDatabaseType(sdxCluster.getSdxDatabase()).orElse(null)));

            if (previousDatabase != null && previousDatabaseConfig != null
                    && ((StringUtils.isNotEmpty(previousDatabaseConfig.getInstanceType())
                    && !previousDatabaseConfig.getInstanceType().equals(previousDatabase.getInstanceType()))
                    || (previousDatabaseConfig.getVolumeSize() != previousDatabase.getStorageSize()))) {
                return Optional.of(previousDatabase);
            }
        }
        return Optional.empty();
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn,
            SdxDatabaseOperation sdxDatabaseOperation, boolean cancellable) {
        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS, durationInMinutes, TimeUnit.MINUTES);
        return waitAndGetDatabase(sdxCluster, databaseCrn, pollingConfig, sdxDatabaseOperation, cancellable);
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn, PollingConfig pollingConfig,
            SdxDatabaseOperation sdxDatabaseOperation, boolean cancellable) {
        return Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccurred())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> {
                    if (cancellable && PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                        LOGGER.info("Database wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                        return AttemptResults.breakFor("Database wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                    }
                    try {
                        LOGGER.info("Creation polling redbeams for database status: '{}' in '{}' env",
                                sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        DatabaseServerStatusV4Response rdsStatus = getDatabaseStatus(databaseCrn);
                        LOGGER.info("Response from redbeams: {}", JsonUtil.writeValueAsString(rdsStatus));
                        if (sdxDatabaseOperation.getExitCriteria().apply(rdsStatus.getStatus())) {
                            return AttemptResults.finishWith(rdsStatus);
                        } else {
                            if (sdxDatabaseOperation.getFailureCriteria().apply(rdsStatus.getStatus())) {
                                if (rdsStatus.getStatusReason() != null && rdsStatus.getStatusReason().contains("does not exist")) {
                                    return AttemptResults.finishWith(null);
                                }
                                return AttemptResults.breakFor("Database operation failed " + sdxCluster.getEnvName()
                                        + " statusReason: " + rdsStatus.getStatusReason());
                            } else {
                                return AttemptResults.justContinue();
                            }
                        }
                    } catch (NotFoundException e) {
                        return AttemptResults.finishWith(null);
                    }
                });
    }

    public OperationView getOperationProgressStatus(String databaseCrn, boolean detailed) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> operationV4Endpoint.getRedbeamsOperationProgressByResourceCrn(databaseCrn, detailed));
    }

    private DatabaseServerStatusV4Response getDatabaseStatus(String databaseCrn) {
        DatabaseServerV4Response response = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> databaseServerV4Endpoint.getByCrn(databaseCrn));
        DatabaseServerStatusV4Response statusResponse = new DatabaseServerStatusV4Response();
        statusResponse.setEnvironmentCrn(response.getEnvironmentCrn());
        statusResponse.setName(response.getName());
        statusResponse.setResourceCrn(response.getCrn());
        statusResponse.setStatus(response.getStatus());
        statusResponse.setStatusReason(response.getStatusReason());
        return statusResponse;
    }

    public StackDatabaseServerResponse getDatabaseServer(String userCrn, String clusterCrn) {
        SdxCluster sdxCluster = sdxService.getByCrn(userCrn, clusterCrn);
        if (sdxCluster.getDatabaseCrn() == null) {
            throw com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound("Database for Data Lake with Data Lake crn:", clusterCrn).get();
        }
        return getDatabaseServer(sdxCluster.getDatabaseCrn());
    }

    public StackDatabaseServerResponse getDatabaseServer(String databaseServerCrn) {
        DatabaseServerV4Response databaseServerV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> databaseServerV4Endpoint.getByCrn(databaseServerCrn));

        return databaseServerConverter.convert(databaseServerV4Response);
    }

    public DatabaseServerV4Response getDatabaseServerV4Response(String databaseServerCrn) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                () -> databaseServerV4Endpoint.getByCrn(databaseServerCrn));
    }

    public SdxDatabase updateDatabaseTypeFromRedbeams(SdxDatabase sdxDatabase) {
        DatabaseServerV4Response databaseServerV4Response = StringUtils.isNotBlank(sdxDatabase.getDatabaseCrn()) ?
                getDatabaseServerV4Response(sdxDatabase.getDatabaseCrn()) : null;
        LOGGER.debug("Describe response from redbeams: {}", databaseServerV4Response);
        if (databaseServerV4Response != null && databaseServerV4Response.getDatabasePropertiesV4Response() != null
                && StringUtils.isNotBlank(databaseServerV4Response.getDatabasePropertiesV4Response().getDatabaseType())) {
            AzureDatabaseType azureDbTypeInRedbeams = AzureDatabaseType.safeValueOf(
                    databaseServerV4Response.getDatabasePropertiesV4Response().getDatabaseType());
            AzureDatabaseType azureDatabaseTypeInSdx = azureDatabaseAttributesService.getAzureDatabaseType(sdxDatabase);
            LOGGER.debug("DB type in redbeams is [{}] and in SDX [{}]", azureDbTypeInRedbeams, azureDatabaseTypeInSdx);
            if (FLEXIBLE_SERVER.equals(azureDbTypeInRedbeams) && SINGLE_SERVER.equals(azureDatabaseTypeInSdx)) {
                azureDatabaseAttributesService.updateDatabaseType(sdxDatabase, azureDbTypeInRedbeams);
                LOGGER.info("Updating database type from redbeams [{}] to sdx database [{}]", azureDbTypeInRedbeams, azureDatabaseTypeInSdx);
                return sdxDatabaseRepository.save(sdxDatabase);
            } else {
                LOGGER.debug("No update was required");
                return sdxDatabase;
            }
        } else {
            LOGGER.debug("No update was required");
            return sdxDatabase;
        }
    }
}