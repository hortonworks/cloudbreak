package com.sequenceiq.cloudbreak.core.flow2.externaldatabase;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
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

    private final  Map<CloudPlatform, DatabaseStackConfig> dbConfigs;

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
        AllocateDatabaseServerV4Request request = getDatabaseRequest(environment, externalDatabase);
        String databaseCrn;
        try {
            databaseCrn = redbeamsClient.create(request).getResourceCrn();
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
            DatabaseServerV4Response response = redbeamsClient.deleteByCrn(cluster.getDatabaseServerCrn(), forced);
            waitAndGetDatabase(cluster, response.getCrn(), DatabaseOperation.DELETION, false);
        } catch (NotFoundException notFoundException) {
            LOGGER.info("Database server is deleted on redbeams side {}", cluster.getDatabaseServerCrn());
        }
    }

    private void updateClusterWithDatabaseServerCrn(Cluster cluster, String databaseServerCrn) {
        cluster.setDatabaseServerCrn(databaseServerCrn);
        clusterRepository.save(cluster);
    }

    private AllocateDatabaseServerV4Request getDatabaseRequest(DetailedEnvironmentResponse environment, DatabaseAvailabilityType externalDatabase) {
        AllocateDatabaseServerV4Request req = new AllocateDatabaseServerV4Request();
        req.setEnvironmentCrn(environment.getCrn());
        req.setDatabaseServer(getDatabaseServerStackRequest(CloudPlatform.valueOf(environment.getCloudPlatform().toUpperCase(Locale.US)), externalDatabase));
        return req;
    }

    private DatabaseServerV4StackRequest getDatabaseServerStackRequest(CloudPlatform cloudPlatform, DatabaseAvailabilityType externalDatabase) {
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
