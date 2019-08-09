package com.sequenceiq.datalake.service.sdx;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTerminationOutcomeV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.client.RedbeamsServiceCrnClient;

@Service
public class DatabaseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

    private static final int SLEEP_TIME_IN_SEC_FOR_ENV_POLLING = 10;

    private static final int DURATION_IN_MINUTES_FOR_ENV_POLLING = 60;

    private static final long STORAGE_SIZE = 40L;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private RedbeamsServiceCrnClient redbeamsClient;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    public DatabaseServerStatusV4Response create(SdxCluster sdxCluster, DetailedEnvironmentResponse env, String requestId) {
        LOGGER.info("Create databaseServer in environment {} for SDX {}", env.getName(), sdxCluster.getClusterName());
        String dbResourceCrn;
        if (dbHasBeenCreatedPreviously(sdxCluster)) {
            dbResourceCrn = sdxCluster.getDatabaseCrn();
        } else {
            dbResourceCrn = redbeamsClient
                    .withCrn(threadBasedUserCrnProvider.getUserCrn())
                    .databaseServerV4Endpoint().create(getDatabaseRequest(env))
                    .getResourceCrn();
            sdxCluster.setDatabaseCrn(dbResourceCrn);
        }
        sdxCluster.setStatus(SdxClusterStatus.EXTERNAL_DATABASE_CREATION_IN_PROGRESS);
        sdxClusterRepository.save(sdxCluster);
        return waitAndGetDatabase(sdxCluster, dbResourceCrn,
                Status::isAvailable, status -> status.isDeleteInProgressOrCompleted() || Status.CREATE_FAILED.equals(status), requestId);
    }

    private boolean dbHasBeenCreatedPreviously(SdxCluster sdxCluster) {
        return Strings.isNotEmpty(sdxCluster.getDatabaseCrn());
    }

    private void saveStatus(SdxCluster cluster, SdxClusterStatus status) {
        cluster.setStatus(status);
        sdxClusterRepository.save(cluster);
    }

    public void terminate(SdxCluster sdxCluster, String requestId) {
        LOGGER.info("Terminating databaseServer of SDX {}", sdxCluster.getClusterName());
        try {
            DatabaseServerTerminationOutcomeV4Response resp = redbeamsClient
                    .withCrn(threadBasedUserCrnProvider.getUserCrn())
                    .databaseServerV4Endpoint().terminate(sdxCluster.getDatabaseCrn());
            saveStatus(sdxCluster, SdxClusterStatus.EXTERNAL_DATABASE_DELETION_IN_PROGRESS);
            waitAndGetDatabase(sdxCluster, resp.getResourceCrn(), Status.DELETE_COMPLETED::equals,
                    Status.DELETE_FAILED::equals, requestId);
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server is deleted on redbeams side {}", sdxCluster.getDatabaseCrn());
        }
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(DetailedEnvironmentResponse env) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        req.setEnvironmentCrn(env.getCrn());
        req.setDatabaseServer(getDatabaseServerRequest(env));
        return req;
    }

    private DatabaseServerV4StackRequest getDatabaseServerRequest(DetailedEnvironmentResponse env) {
        DatabaseServerV4StackRequest req = new DatabaseServerV4StackRequest();
        req.setInstanceType("db.m3.medium");
        req.setDatabaseVendor("postgres");
        req.setStorageSize(STORAGE_SIZE);
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
            Function<Status, Boolean> exitcrit, Function<Status, Boolean> failurecrit, String requestId) {
        PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC_FOR_ENV_POLLING, TimeUnit.SECONDS,
                DURATION_IN_MINUTES_FOR_ENV_POLLING, TimeUnit.MINUTES);
        return waitAndGetDatabase(sdxCluster, databaseCrn, pollingConfig, exitcrit, failurecrit, requestId);
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn, PollingConfig pollingConfig,
            Function<Status, Boolean> exitcrit, Function<Status, Boolean> failurecrit, String requestId) {
        return Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(false)
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> {
                    try {
                        MDCBuilder.addRequestIdToMdcContext(requestId);
                        LOGGER.info("Creation polling redbeams for database status: '{}' in '{}' env",
                                sdxCluster.getClusterName(), sdxCluster.getEnvName());
                        DatabaseServerStatusV4Response rdsStatus = getDatabaseStatus(databaseCrn);
                        LOGGER.info("Response from redbeams: {}", JsonUtil.writeValueAsString(rdsStatus));
                        if (exitcrit.apply(rdsStatus.getStatus())) {
                            return AttemptResults.finishWith(rdsStatus);
                        } else {
                            if (failurecrit.apply(rdsStatus.getStatus())) {
                                if (rdsStatus.getStatusReason() != null && rdsStatus.getStatusReason().startsWith("No databaseserver found with crn")) {
                                    return AttemptResults.finishWith(null);
                                }
                                return AttemptResults.breakFor("Database operation failed " + sdxCluster.getEnvName());
                            } else {
                                return AttemptResults.justContinue();
                            }
                        }
                    } catch (NotFoundException e) {
                        return AttemptResults.finishWith(null);
                    }
                });
    }

    private DatabaseServerStatusV4Response getDatabaseStatus(String databaseCrn) {
        return redbeamsClient.withCrn(threadBasedUserCrnProvider.getUserCrn())
                .databaseServerV4Endpoint().getStatusOfManagedDatabaseServerByCrn(databaseCrn);
    }
}
