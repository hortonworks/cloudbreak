package com.sequenceiq.datalake.service.sdx;

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
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.statestore.DatalakeInMemoryStateStore;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTerminationOutcomeV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Service
public class DatabaseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

    private static final int SLEEP_TIME_IN_SEC_FOR_ENV_POLLING = 10;

    private static final int DURATION_IN_MINUTES_FOR_ENV_POLLING = 60;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private SdxNotificationService notificationService;

    @Inject
    private SdxStatusService sdxStatusService;

    @Inject
    private Map<SdxClusterShape, DatabaseConfig> dbConfigs;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    public DatabaseServerStatusV4Response create(SdxCluster sdxCluster, DetailedEnvironmentResponse env, String requestId) {
        LOGGER.info("Create databaseServer in environment {} for SDX {}", env.getName(), sdxCluster.getClusterName());
        String dbResourceCrn;
        if (dbHasBeenCreatedPreviously(sdxCluster)) {
            dbResourceCrn = sdxCluster.getDatabaseCrn();
        } else {
            try {
                dbResourceCrn = databaseServerV4Endpoint.create(getDatabaseRequest(sdxCluster.getClusterShape(), env))
                        .getResourceCrn();
                sdxCluster.setDatabaseCrn(dbResourceCrn);
                sdxClusterRepository.save(sdxCluster);
                sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.EXTERNAL_DATABASE_CREATION_IN_PROGRESS,
                        "External database creation in progress", sdxCluster);
            } catch (BadRequestException badRequestException) {
                LOGGER.error("Redbeams create request failed, bad request", badRequestException);
                throw badRequestException;
            }
        }
        notificationService.send(ResourceEvent.SDX_RDS_CREATION_STARTED, sdxCluster);
        return waitAndGetDatabase(sdxCluster, dbResourceCrn, SdxDatabaseOperation.CREATION, requestId, true);
    }

    private boolean dbHasBeenCreatedPreviously(SdxCluster sdxCluster) {
        return Strings.isNotEmpty(sdxCluster.getDatabaseCrn());
    }

    public void terminate(SdxCluster sdxCluster, String requestId) {
        LOGGER.info("Terminating databaseServer of SDX {}", sdxCluster.getClusterName());
        try {
            DatabaseServerTerminationOutcomeV4Response resp = databaseServerV4Endpoint.terminate(sdxCluster.getDatabaseCrn(), true);
            sdxStatusService.setStatusForDatalake(DatalakeStatusEnum.EXTERNAL_DATABASE_DELETION_IN_PROGRESS,
                    "External database deletion in progress", sdxCluster);
            notificationService.send(ResourceEvent.SDX_RDS_DELETION_STARTED, sdxCluster);
            waitAndGetDatabase(sdxCluster, resp.getResourceCrn(), SdxDatabaseOperation.DELETION, requestId, false);
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server is deleted on redbeams side {}", sdxCluster.getDatabaseCrn());
        }
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(SdxClusterShape clusterShape, DetailedEnvironmentResponse env) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        req.setEnvironmentCrn(env.getCrn());
        req.setDatabaseServer(getDatabaseServerRequest(clusterShape));
        return req;
    }

    private DatabaseServerV4StackRequest getDatabaseServerRequest(SdxClusterShape clusterShape) {
        DatabaseConfig databaseConfig = dbConfigs.get(clusterShape);
        if (databaseConfig == null) {
            throw new BadRequestException("Not found database config for " + clusterShape);
        }
        DatabaseServerV4StackRequest req = new DatabaseServerV4StackRequest();
        req.setInstanceType(databaseConfig.getInstanceType());
        req.setDatabaseVendor(databaseConfig.getVendor());
        req.setStorageSize(databaseConfig.getVolumeSize());
        req.setAws(getAwsDatabaseServerParameters());
        return req;
    }

    private AwsDatabaseServerV4Parameters getAwsDatabaseServerParameters() {
        AwsDatabaseServerV4Parameters params = new AwsDatabaseServerV4Parameters();
        params.setBackupRetentionPeriod(1);
        params.setEngineVersion("10.6");
        return params;
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn,
            SdxDatabaseOperation sdxDatabaseOperation, String requestId, boolean cancellable) {
        PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC_FOR_ENV_POLLING, TimeUnit.SECONDS,
                DURATION_IN_MINUTES_FOR_ENV_POLLING, TimeUnit.MINUTES);
        return waitAndGetDatabase(sdxCluster, databaseCrn, pollingConfig, sdxDatabaseOperation, requestId, cancellable);
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn, PollingConfig pollingConfig,
            SdxDatabaseOperation sdxDatabaseOperation, String requestId, boolean cancellable) {
        DatabaseServerStatusV4Response response = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> {
                    if (cancellable && PollGroup.CANCELLED.equals(DatalakeInMemoryStateStore.get(sdxCluster.getId()))) {
                        LOGGER.info("Database wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                        return AttemptResults.breakFor("Database wait polling cancelled in inmemory store, id: " + sdxCluster.getId());
                    }
                    try {
                        MDCBuilder.addRequestId(requestId);
                        LOGGER.info("Creation polling redbeams for database status: '{}' in '{}' env",
                                sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        DatabaseServerStatusV4Response rdsStatus = getDatabaseStatus(databaseCrn);
                        LOGGER.info("Response from redbeams: {}", JsonUtil.writeValueAsString(rdsStatus));
                        if (sdxDatabaseOperation.getExitCriteria().apply(rdsStatus.getStatus())) {
                            return AttemptResults.finishWith(rdsStatus);
                        } else {
                            if (sdxDatabaseOperation.getFailureCriteria().apply(rdsStatus.getStatus())) {
                                notificationService.send(sdxDatabaseOperation.getFailedEvent(), sdxCluster);
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
        notificationService.send(sdxDatabaseOperation.getFinishedEvent(), sdxCluster);
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
