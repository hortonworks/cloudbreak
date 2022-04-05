package com.sequenceiq.datalake.service.sdx.database;

import static com.sequenceiq.datalake.service.TagUtil.getTags;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.datalake.configuration.PlatformConfig;
import com.sequenceiq.datalake.converter.DatabaseServerConverter;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxDatabaseOperation;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
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

@Service
public class DatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

    private static final String SSL_ENFORCEMENT_MIN_RUNTIME_VERSION = "7.2.2";

    private final Comparator<Versioned> versionComparator = new VersionComparator();

    @Value("${sdx.db.operation.sleeptime_sec:10}")
    private int sleepTimeInSec;

    @Value("${sdx.db.operation.duration_min:80}")
    private int durationInMinutes;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

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
    private EntitlementService entitlementService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

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
                        regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                        () -> databaseServerV4Endpoint.createInternal(getDatabaseRequest(sdxCluster, env), initiatorUserCrn));
                dbResourceCrn = serverStatusV4Response.getResourceCrn();
                sdxCluster.setDatabaseCrn(dbResourceCrn);
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
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> databaseServerV4Endpoint.stop(databaseCrn));
            waitAndGetDatabase(sdxCluster, databaseCrn, SdxDatabaseOperation.STOP, false);
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(SdxCluster sdxCluster, DetailedEnvironmentResponse env) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        String environmentCrn = env.getCrn();
        req.setEnvironmentCrn(environmentCrn);
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(env.getCloudPlatform().toUpperCase(Locale.US));
        req.setDatabaseServer(getDatabaseServerRequest(cloudPlatform, sdxCluster));
        req.setTags(getTags(sdxCluster.getTags()));
        req.setClusterCrn(sdxCluster.getCrn());

        String runtime = sdxCluster.getRuntime();
        if (platformConfig.isExternalDatabaseSslEnforcementSupportedFor(cloudPlatform) && isSslEnforcementSupportedForRuntime(runtime)
                && entitlementService.databaseWireEncryptionEnabled(Crn.safeFromString(environmentCrn).getAccountId())) {
            LOGGER.info("Applying external DB SSL enforcement for cloud platform {} and runtime version {}", cloudPlatform, runtime);
            SslConfigV4Request sslConfigV4Request = new SslConfigV4Request();
            sslConfigV4Request.setSslMode(SslMode.ENABLED);
            req.setSslConfig(sslConfigV4Request);
        } else {
            LOGGER.info("Skipping external DB SSL enforcement for cloud platform {} and runtime version {}", cloudPlatform, runtime);
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

    private DatabaseServerV4StackRequest getDatabaseServerRequest(CloudPlatform cloudPlatform, SdxCluster sdxCluster) {
        DatabaseConfig databaseConfig = dbConfigs.get(new DatabaseConfigKey(cloudPlatform, sdxCluster.getClusterShape()));
        if (databaseConfig == null) {
            throw new BadRequestException("Database config for cloud platform " + cloudPlatform + ", cluster shape "
                    + sdxCluster.getClusterShape() + " not found");
        }
        DatabaseServerV4StackRequest req = new DatabaseServerV4StackRequest();
        req.setInstanceType(databaseConfig.getInstanceType());
        req.setDatabaseVendor(databaseConfig.getVendor());
        req.setStorageSize(databaseConfig.getVolumeSize());
        databaseServerParameterSetterMap.get(cloudPlatform).setParameters(req, sdxCluster.getDatabaseAvailabilityType(), sdxCluster.getDatabaseEngineVersion());
        return req;
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn,
            SdxDatabaseOperation sdxDatabaseOperation, boolean cancellable) {
        PollingConfig pollingConfig = new PollingConfig(sleepTimeInSec, TimeUnit.SECONDS,
                durationInMinutes, TimeUnit.MINUTES);
        return waitAndGetDatabase(sdxCluster, databaseCrn, pollingConfig, sdxDatabaseOperation, cancellable);
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn, PollingConfig pollingConfig,
            SdxDatabaseOperation sdxDatabaseOperation, boolean cancellable) {
        DatabaseServerStatusV4Response response = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
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
        return response;
    }

    public OperationView getOperationProgressStatus(String databaseCrn, boolean detailed) {
        return ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> operationV4Endpoint.getRedbeamsOperationProgressByResourceCrn(databaseCrn, detailed));
    }

    private DatabaseServerStatusV4Response getDatabaseStatus(String databaseCrn) {
        DatabaseServerV4Response response = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
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
        DatabaseServerV4Response databaseServerV4Response = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> databaseServerV4Endpoint.getByCrn(sdxCluster.getDatabaseCrn()));

        return databaseServerConverter.convert(databaseServerV4Response);
    }
}
