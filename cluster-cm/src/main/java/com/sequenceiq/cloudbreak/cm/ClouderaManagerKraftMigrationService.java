package com.sequenceiq.cloudbreak.cm;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiService;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.cm.polling.ClouderaManagerPollingServiceProvider;
import com.sequenceiq.cloudbreak.cm.util.ClouderaManagerConstants;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class ClouderaManagerKraftMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerKraftMigrationService.class);

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String KAFKA_BROKER_ROLE = "KAFKA_BROKER";

    private static final String KAFKA_CONNECT_ROLE = "KAFKA_CONNECT";

    private static final String KRAFT_MIGRATION_COMMAND_NAME = "KRaftMigrationCommand";

    private static final String KRAFT_ROLE_TYPE = "KRAFT";

    private static final String KRAFT_PROPERTIES_ROLE_SAFETY_VALVE = "kraft.properties_role_safety_valve";

    private static final String ZOOKEEPER_MIGRATION_ENABLE_CONF_VALUE = "zookeeper.metadata.migration.enable=true";

    @Inject
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Inject
    private ClouderaManagerPollingServiceProvider clouderaManagerPollingServiceProvider;

    @Inject
    private ClouderaManagerConfigService configService;

    @Inject
    private ClouderaManagerRestartService clouderaManagerRestartService;

    public void enableZookeeperMigrationMode(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        String clusterName = stackDtoDelegate.getCluster().getName();
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(client);
        configService.getServiceName(clusterName, KAFKA_SERVICE_TYPE, servicesResourceApi)
            .ifPresentOrElse(serviceName -> {
                try {
                    String roleConfigGroupName = configService.getRoleConfigGroupNameByTypeAndServiceName(KRAFT_ROLE_TYPE, clusterName, serviceName,
                            roleConfigGroupsResourceApi);
                    configService.modifyRoleConfigGroup(client, clusterName, serviceName, roleConfigGroupName,
                            Map.of(KRAFT_PROPERTIES_ROLE_SAFETY_VALVE, ZOOKEEPER_MIGRATION_ENABLE_CONF_VALUE));
                } catch (ApiException e) {
                    LOGGER.debug("Error when retrieving {} for service {} in cluster {}.", KRAFT_ROLE_TYPE, serviceName, clusterName, e);
                    throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
                }
            }, () -> {
                LOGGER.info("{} service name is missing, skip modifying the {} property.", KAFKA_SERVICE_TYPE, KRAFT_PROPERTIES_ROLE_SAFETY_VALVE);
                throw new ClouderaManagerOperationFailedException(String.format("Service of type: %s is not found", KAFKA_SERVICE_TYPE));
            });
    }

    public void restartKafkaBrokerNodes(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        LOGGER.info("Restarting Kafka Broker nodes for cluster {}.", stackDtoDelegate.getCluster().getName());
        clouderaManagerRestartService.restartServiceRolesByType(stackDtoDelegate, client, KAFKA_SERVICE_TYPE, KAFKA_BROKER_ROLE);
    }

    public void restartKafkaConnectNodes(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        LOGGER.info("Restarting Kafka Connect nodes for cluster {}.", stackDtoDelegate.getCluster().getName());
        clouderaManagerRestartService.restartServiceRolesByType(stackDtoDelegate, client, KAFKA_SERVICE_TYPE, KAFKA_CONNECT_ROLE);
    }

    public void migrateZookeeperToKraft(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(client);
        String clusterName = stackDtoDelegate.getCluster().getName();

        try {
            Collection<ApiService> apiServices = readServices(client, stackDtoDelegate);

            Optional<ApiService> optionalApiService = apiServices.stream()
                    .filter(service -> KAFKA_SERVICE_TYPE.equals(service.getType()))
                    .findFirst();

            if (optionalApiService.isPresent()) {
                ApiService service = optionalApiService.get();
                LOGGER.info("Migrating Zookeeper to KRaft. Calling /clusters/{}/services/{}/commands/{} CM endpoint",
                        clusterName, service.getName(), KRAFT_MIGRATION_COMMAND_NAME);

                ApiCommand kraftMigrationCommand = api.serviceCommandByName(
                        clusterName, KRAFT_MIGRATION_COMMAND_NAME, service.getName());

                pollKraftMigrationCommand(client, stackDtoDelegate, kraftMigrationCommand.getId());
            } else {
                LOGGER.error("Cannot migrate Zookeeper to KRaft. No {} service type found for cluster {}",
                        KAFKA_SERVICE_TYPE, clusterName);
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.error("Failed to migrate Zookeeper to KRaft", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void pollKraftMigrationCommand(ApiClient client, StackDtoDelegate stackDtoDelegate, BigDecimal commandId)
            throws CloudbreakException {
        ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider
                .startPollingZookeeperToKraftMigration(stackDtoDelegate, client, commandId);

        if (pollingResult.isExited()) {
            throw new CancellationException(
                    "Cluster was terminated while waiting for command API to be available for Zookeeper to KRaft migration");
        } else if (pollingResult.isTimeout()) {
            throw new CloudbreakException(
                    "Timeout during waiting for command API to be available (Zookeeper to KRaft migration).");
        }
    }

    private Collection<ApiService> readServices(ApiClient client, StackDtoDelegate stack) throws ApiException {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(client);
        return api.readServices(stack.getName(), ClouderaManagerConstants.SUMMARY).getItems();
    }
}