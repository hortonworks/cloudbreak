package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxClusterStatus;
import com.sequenceiq.datalake.repository.SdxClusterRepository;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerStatusV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerTerminationOutcomeV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.AwsDatabaseServerV4Parameters;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.SecurityGroupV4Request;
import com.sequenceiq.redbeams.api.model.common.Status;

@Service
public class DatabaseService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseService.class);

    private static final int SLEEP_TIME_IN_SEC_FOR_ENV_POLLING = 10;

    private static final int DURATION_IN_MINUTES_FOR_ENV_POLLING = 60;

    private static final long STORAGE_SIZE = 40L;

    @Inject
    private SdxClusterRepository sdxClusterRepository;

    @Inject
    private DatabaseServerV4Endpoint databaseServerV4Endpoint;

    public DatabaseServerStatusV4Response create(Long sdxId, Optional<SdxCluster> sdxClusterOptional, DetailedEnvironmentResponse env) {
        if (sdxClusterOptional.isPresent()) {
            DatabaseServerStatusV4Response resp = databaseServerV4Endpoint.create(getDatabaseRequest(env));
            sdxClusterOptional.ifPresent(sdxCluster -> {
                sdxCluster.setDatabaseCrn(resp.getResourceCrn());
                sdxCluster.setStatus(SdxClusterStatus.EXTERNAL_DATABASE_CREATION_IN_PROGRESS);
                sdxClusterRepository.save(sdxCluster);
            });
            DatabaseServerStatusV4Response waitResp = waitAndGetDatabase(sdxClusterOptional.get(), resp.getResourceCrn(),
                    status -> status.isAvailable(), status -> Status.CREATE_FAILED.equals(status));
            return waitResp;
        } else {
            throw notFound("SDX cluster", sdxId).get();
        }
    }

    private void saveStatus(SdxCluster cluster, SdxClusterStatus status) {
        cluster.setStatus(status);
        sdxClusterRepository.save(cluster);
    }

    public void terminate(Long sdxId, Optional<SdxCluster> sdxClusterOptional) {
        sdxClusterOptional.ifPresentOrElse(sdxCluster -> {
            DatabaseServerTerminationOutcomeV4Response resp = databaseServerV4Endpoint.terminate(sdxCluster.getDatabaseCrn());
            saveStatus(sdxClusterOptional.get(), SdxClusterStatus.EXTERNAL_DATABASE_DELETION_IN_PROGRESS);
            waitAndGetDatabase(sdxCluster, resp.getResourceCrn(),
                    status -> Status.DELETE_COMPLETED.equals(status), status -> Status.DELETE_FAILED.equals(status));
        }, () -> {
            throw notFound("SDX cluster", sdxId).get();
        });
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(DetailedEnvironmentResponse env) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        req.setEnvironmentCrn(env.getCrn());
        req.setDatabaseServer(getDatabaseServerRequest(env));
        return req;
    }

    private DatabaseServerV4Request getDatabaseServerRequest(DetailedEnvironmentResponse env) {
        DatabaseServerV4Request req = new DatabaseServerV4Request();
        req.setInstanceType("db.m3.medium");
        req.setDatabaseVendor("postgres");
        req.setStorageSize(STORAGE_SIZE);
        req.setAws(getAwsDatabaseServerParameters());
        req.setSecurityGroup(getSecurityGroupRequest(env));
        return req;
    }

    private SecurityGroupV4Request getSecurityGroupRequest(DetailedEnvironmentResponse env) {
        SecurityGroupV4Request sec = new SecurityGroupV4Request();
        sec.setSecurityGroupIds(Set.of(env.getSecurityAccess().getDefaultSecurityGroupId()));
        return sec;
    }

    private AwsDatabaseServerV4Parameters getAwsDatabaseServerParameters() {
        AwsDatabaseServerV4Parameters params = new AwsDatabaseServerV4Parameters();
        params.setBackupRetentionPeriod(1);
        params.setEngineVersion("10.6");
        return params;
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn,
            Function<Status, Boolean> exitcrit, Function<Status, Boolean> failurecrit) {
        PollingConfig pollingConfig = new PollingConfig(SLEEP_TIME_IN_SEC_FOR_ENV_POLLING, TimeUnit.SECONDS,
                DURATION_IN_MINUTES_FOR_ENV_POLLING, TimeUnit.MINUTES);
        return waitAndGetDatabase(sdxCluster, databaseCrn, pollingConfig, exitcrit, failurecrit);
    }

    public DatabaseServerStatusV4Response waitAndGetDatabase(SdxCluster sdxCluster, String databaseCrn, PollingConfig pollingConfig,
            Function<Status, Boolean> exitcrit, Function<Status, Boolean> failurecrit) {
        DatabaseServerStatusV4Response databaseResponse = Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(false)
                .stopAfterDelay(pollingConfig.getDuration(), pollingConfig.getDurationTimeUnit())
                .run(() -> {
                    try {
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
        return databaseResponse;
    }

    private DatabaseServerStatusV4Response getDatabaseStatus(String databaseCrn) {
        return databaseServerV4Endpoint.getStatusOfManagedDatabaseServerByCrn(databaseCrn);
    }

}
