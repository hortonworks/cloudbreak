package com.sequenceiq.datalake.service.sdx.database;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.PollingConfig;
import com.sequenceiq.datalake.service.sdx.SdxDatabaseOperation;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

@Service
public class DatabaseService {

    public static final int SLEEP_TIME_IN_SEC_FOR_DB_POLLING = 10;

    public static final int DURATION_IN_MINUTES_FOR_DB_POLLING = 60;

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private Map<DatabaseConfigKey, DatabaseConfig> dbConfigs;

    @Inject
    private Map<CloudPlatform, DatabaseServerParameterSetter> databaseServerParameterSetterMap;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    public DatabaseServerStatusV4Response create(SdxCluster sdxCluster, DetailedEnvironmentResponse env) {
        LOGGER.info("Create databaseServer in environment {} for SDX {}", env.getName(), sdxCluster.getClusterName());
        String dbResourceCrn;
        if (dbHasBeenCreatedPreviously(sdxCluster)) {
            dbResourceCrn = sdxCluster.getDatabaseCrn();
        } else {
            try {
                dbResourceCrn = databaseServerV4Endpoint.create(getDatabaseRequest(sdxCluster, env))
                        .getResourceCrn();
                sdxCluster.setDatabaseCrn(dbResourceCrn);
                sdxClusterRepository.save(sdxCluster);
                sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_CREATION_IN_PROGRESS,
                        "External database creation in progress", sdxCluster);
            } catch (BadRequestException badRequestException) {
                LOGGER.error("Redbeams create request failed, bad request", badRequestException);
                throw badRequestException;
            }
        }
        return waitAndGetDatabase(sdxCluster, dbResourceCrn, SdxDatabaseOperation.CREATION, true);
    }

    private boolean dbHasBeenCreatedPreviously(SdxCluster sdxCluster) {
        return Strings.isNotEmpty(sdxCluster.getDatabaseCrn());
    }

    public void terminate(SdxCluster sdxCluster, boolean forced) {
        LOGGER.info("Terminating databaseServer of SDX {}", sdxCluster.getClusterName());
        try {
            DatabaseServerV4Response resp = databaseServerV4Endpoint.deleteByCrn(sdxCluster.getDatabaseCrn(), forced);
            sdxStatusService.setStatusForDatalakeAndNotify(DatalakeStatusEnum.EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
                    "External database deletion in progress", sdxCluster);
            waitAndGetDatabase(sdxCluster, resp.getCrn(), SdxDatabaseOperation.DELETION, false);
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server is deleted on redbeams side {}", sdxCluster.getDatabaseCrn());
        }
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(SdxCluster sdxCluster, DetailedEnvironmentResponse env) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        req.setEnvironmentCrn(env.getCrn());
        req.setDatabaseServer(getDatabaseServerRequest(CloudPlatform.valueOf(env.getCloudPlatform().toUpperCase(Locale.US)), sdxCluster));
        return req;
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
        databaseServerParameterSetterMap.get(cloudPlatform).setParameters(req, sdxCluster.getDatabaseAvailabilityType());
        return req;
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn,
            SdxDatabaseOperation sdxDatabaseOperation, boolean cancellable) {
        PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC_FOR_DB_POLLING, TimeUnit.SECONDS,
                DURATION_IN_MINUTES_FOR_DB_POLLING, TimeUnit.MINUTES);
        return waitAndGetDatabase(sdxCluster, databaseCrn, pollingConfig, sdxDatabaseOperation, cancellable);
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn, PollingConfig pollingConfig,
            SdxDatabaseOperation sdxDatabaseOperation, boolean cancellable) {
        DatabaseServerStatusV4Response response = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
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

    private DatabaseServerStatusV4Response getDatabaseStatus(String databaseCrn) {
        DatabaseServerV4Response response = databaseServerV4Endpoint.getByCrn(databaseCrn);
        DatabaseServerStatusV4Response statusResponse = new DatabaseServerStatusV4Response();
        statusResponse.setEnvironmentCrn(response.getEnvironmentCrn());
        statusResponse.setName(response.getName());
        statusResponse.setResourceCrn(response.getCrn());
        statusResponse.setStatus(response.getStatus());
        statusResponse.setStatusReason(response.getStatusReason());
        return statusResponse;
    }
}
