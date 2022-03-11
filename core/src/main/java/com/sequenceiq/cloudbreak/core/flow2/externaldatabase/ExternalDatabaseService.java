package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.cloud.model.StackTags;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseOperation;
import com.sequenceiq.cloudbreak.service.externaldatabase.DatabaseServerParameterDecorator;
import com.sequenceiq.cloudbreak.service.externaldatabase.PollingConfig;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseStackConfig;
import com.sequenceiq.cloudbreak.service.rdsconfig.RedbeamsClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.AllocateDatabaseServerV4Request;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;

@Service
public class ExternalDatabaseService {

    public static final int SLEEP_TIME_IN_SEC_FOR_DB_POLLING = 10;

    public static final int DURATION_IN_MINUTES_FOR_DB_POLLING = 60;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalDatabaseService.class);

    private final RedbeamsClientService redbeamsClient;

    private final ClusterRepository clusterRepository;

    private final Map<CloudPlatform, DatabaseStackConfig> dbConfigs;

    private final Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap;

    private final DatabaseObtainerService databaseObtainerService;

    public ExternalDatabaseService(RedbeamsClientService redbeamsClient, ClusterRepository clusterRepository,
            Map<CloudPlatform, DatabaseStackConfig> dbConfigs, Map<CloudPlatform, DatabaseServerParameterDecorator> parameterDecoratorMap,
            DatabaseObtainerService databaseObtainerService) {
        this.redbeamsClient = redbeamsClient;
        this.clusterRepository = clusterRepository;
        this.dbConfigs = dbConfigs;
        this.parameterDecoratorMap = parameterDecoratorMap;
        this.databaseObtainerService = databaseObtainerService;
    }

    public void provisionDatabase(Cluster cluster, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        LOGGER.info("Create external {} database server in environment {} for DataHub {}", externalDatabase.name(), environment.getName(), cluster.getName());
        String databaseCrn;
        try {
            Optional<DatabaseServerV4Response> existingDatabase = findExistingDatabase(cluster, environment.getCrn());
            if (existingDatabase.isPresent()) {
                String dbCrn = existingDatabase.get().getCrn();
                LOGGER.debug("Found existing database with CRN {}", dbCrn);
                databaseCrn = dbCrn;
            } else {
                LOGGER.debug("Requesting new database server creation");
                AllocateDatabaseServerV4Request request = getDatabaseRequest(environment, externalDatabase, cluster);
                databaseCrn = redbeamsClient.create(request).getResourceCrn();
            }
            updateClusterWithDatabaseServerCrn(cluster, databaseCrn);
        } catch (BadRequestException badRequestException) {
            LOGGER.error("Redbeams create request failed, bad request", badRequestException);
            throw badRequestException;
        }
        waitAndGetDatabase(cluster, databaseCrn, DatabaseOperation.CREATION, true);
    }

    public void terminateDatabase(Cluster cluster, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment, boolean forced) {
        LOGGER.info("Terminate external {} database server in environment {} for DataHub {}",
                externalDatabase.name(), environment.getName(), cluster.getName());
        try {
            if (externalDatabaseReferenceExist(cluster.getDatabaseServerCrn())) {
                DatabaseServerV4Response response = redbeamsClient.deleteByCrn(cluster.getDatabaseServerCrn(), forced);
                waitAndGetDatabase(cluster, response.getCrn(), DatabaseOperation.DELETION, false);
            } else {
                LOGGER.warn("[INVESTIGATE] The external database type was {} but there was no crn", externalDatabase);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server is deleted on redbeams side {}", cluster.getDatabaseServerCrn());
        }
    }

    public void startDatabase(Cluster cluster, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        LOGGER.info("Starting external {} database server in environment {} for DataHub {}",
                externalDatabase.name(), environment.getName(), cluster.getName());
        String databaseCrn = cluster.getDatabaseServerCrn();
        try {
            if (externalDatabaseReferenceExist(databaseCrn)) {
                redbeamsClient.startByCrn(databaseCrn);
                waitAndGetDatabase(cluster, databaseCrn, DatabaseOperation.START, false);
            } else {
                LOGGER.warn("[INVESTIGATE] The external database type was {} but there was no crn", externalDatabase);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    public void stopDatabase(Cluster cluster, DatabaseAvailabilityType externalDatabase, DetailedEnvironmentResponse environment) {
        LOGGER.info("Stopping external {} database server in environment {} for DataHub {}",
                externalDatabase.name(), environment.getName(), cluster.getName());
        String databaseCrn = cluster.getDatabaseServerCrn();
        try {
            if (externalDatabaseReferenceExist(databaseCrn)) {
                redbeamsClient.stopByCrn(databaseCrn);
                waitAndGetDatabase(cluster, databaseCrn, DatabaseOperation.STOP, false);
            } else {
                LOGGER.warn("[INVESTIGATE] The external database type was {} but there was no crn", externalDatabase);
            }
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server not found on redbeams side {}", databaseCrn);
        }
    }

    private Optional<DatabaseServerV4Response> findExistingDatabase(Cluster cluster, String environmentCrn) {
        if (cluster.getStack() != null) {
            String clusterCrn = cluster.getStack().getResourceCrn();
            LOGGER.debug("Trying to find existing database server for environment {} and cluster {}", environmentCrn, clusterCrn);
            try {
                return Optional.ofNullable(redbeamsClient.getByClusterCrn(environmentCrn, clusterCrn));
            } catch (NotFoundException ignore) {
                LOGGER.debug("External database in environment {} for cluster {} does not exist.", environmentCrn, cluster);
                return Optional.empty();
            }
        }
        LOGGER.warn("[INVESTIGATE] Stack is empty for cluster '{}' in environment {}", cluster.getName(), environmentCrn);
        return Optional.empty();
    }

    private boolean externalDatabaseReferenceExist(String databaseCrn) {
        return !Strings.isNullOrEmpty(databaseCrn);
    }

    private void updateClusterWithDatabaseServerCrn(Cluster cluster, String databaseServerCrn) {
        cluster.setDatabaseServerCrn(databaseServerCrn);
        clusterRepository.save(cluster);
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(DetailedEnvironmentResponse environment, DatabaseAvailabilityType externalDatabase,
            Cluster cluster) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        req.setEnvironmentCrn(environment.getCrn());
        CloudPlatform cloudPlatform = CloudPlatform.valueOf(environment.getCloudPlatform().toUpperCase(Locale.US));
        String databaseEngineVersion = Optional.ofNullable(cluster).map(Cluster::getStack).map(Stack::getExternalDatabaseEngineVersion).orElse(null);
        req.setDatabaseServer(getDatabaseServerStackRequest(cloudPlatform, externalDatabase, databaseEngineVersion));
        if (cluster.getStack() != null) {
            req.setClusterCrn(cluster.getStack().getResourceCrn());
            req.setTags(getUserDefinedTags(cluster.getStack()));
        }
        return req;
    }

    private Map<String, String> getUserDefinedTags(Stack stack) {
        Map<String, String> userDefinedTags = new HashMap<>();
        if (stack.getTags() != null) {
            try {
                StackTags stackTag = stack.getTags().get(StackTags.class);
                if (stackTag != null) {
                    userDefinedTags = stackTag.getUserDefinedTags();
                }
            } catch (IOException e) {
                LOGGER.warn("Stack related applications tags cannot be parsed, use default service type for metering.", e);
            }
        }
        return Objects.requireNonNullElse(userDefinedTags, new HashMap<>());
    }

    private DatabaseServerV4StackRequest getDatabaseServerStackRequest(CloudPlatform cloudPlatform, DatabaseAvailabilityType externalDatabase,
            String databaseEngineVersion) {
        DatabaseStackConfig databaseStackConfig = dbConfigs.get(cloudPlatform);
        if (databaseStackConfig == null) {
            throw new BadRequestException("Database config for cloud platform " + cloudPlatform + " not found");
        }
        DatabaseServerV4StackRequest request = new DatabaseServerV4StackRequest();
        request.setInstanceType(databaseStackConfig.getInstanceType());
        request.setDatabaseVendor(databaseStackConfig.getVendor());
        request.setStorageSize(databaseStackConfig.getVolumeSize());
        DatabaseServerParameter serverParameter = DatabaseServerParameter.builder()
                .withHighlyAvailable(DatabaseAvailabilityType.HA == externalDatabase)
                .withEngineVersion(databaseEngineVersion)
                .build();
        parameterDecoratorMap.get(cloudPlatform).setParameters(request, serverParameter);
        return request;
    }

    private void waitAndGetDatabase(Cluster cluster, String databaseCrn,
            DatabaseOperation databaseOperation, boolean cancellable) {

        PollingConfig pollingConfig = PollingConfig.builder()
                .withSleepTime(SLEEP_TIME_IN_SEC_FOR_DB_POLLING)
                .withSleepTimeUnit(TimeUnit.SECONDS)
                .withTimeout(DURATION_IN_MINUTES_FOR_DB_POLLING)
                .withTimeoutTimeUnit(TimeUnit.MINUTES)
                .withStopPollingIfExceptionOccured(false)
                .build();
        waitAndGetDatabase(cluster, databaseCrn, pollingConfig, databaseOperation, cancellable);
    }

    private void waitAndGetDatabase(Cluster cluster, String databaseCrn, PollingConfig pollingConfig, DatabaseOperation databaseOperation,
            boolean cancellable) {
        Polling.waitPeriodly(pollingConfig.getSleepTime(), pollingConfig.getSleepTimeUnit())
                .stopIfException(pollingConfig.getStopPollingIfExceptionOccured())
                .stopAfterDelay(pollingConfig.getTimeout(), pollingConfig.getTimeoutTimeUnit())
                .run(() -> databaseObtainerService.obtainAttemptResult(cluster, databaseOperation, databaseCrn, cancellable));
    }

}
