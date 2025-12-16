package com.sequenceiq.cloudbreak.cm;

import static org.apache.commons.lang3.BooleanUtils.isFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiService;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.cm.commands.SyncApiCommandRetriever;
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

    private static final String KRAFT_MIGRATION_COMMAND_NAME = "KRaftMigrationCommand";

    private static final String KRAFT_FINALIZE_MIGRATION_COMMAND_NAME = "KRaftFinalizeMigrationCommand";

    private static final String KRAFT_ROLLBACK_MIGRATION_COMMAND_NAME = "KRaftRollbackMigrationCommand";

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
    private SyncApiCommandRetriever syncApiCommandRetriever;

    @Inject
    private ClouderaManagerCommandsService clouderaManagerCommandsService;

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

    public void finalizeZookeeperToKraftMigration(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        String clusterName = stackDtoDelegate.getCluster().getName();
        LOGGER.info("{} command initiated for cluster {}", KRAFT_FINALIZE_MIGRATION_COMMAND_NAME, clusterName);

        try {
            Optional<ApiService> optionalKafkaService = findKafkaService(client, clusterName);

            if (optionalKafkaService.isPresent()) {
                executeOrRetryKraftMigrationCommand(client, optionalKafkaService.get(), stackDtoDelegate, KRAFT_FINALIZE_MIGRATION_COMMAND_NAME);
            } else {
                LOGGER.warn("Cannot finalize Zookeeper to KRaft migration. No {} service type found for cluster {}",
                        KAFKA_SERVICE_TYPE, clusterName);
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.error("Failed to finalize Zookeeper to KRaft migration", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    public void migrateZookeeperToKraft(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        String clusterName = stackDtoDelegate.getCluster().getName();
        LOGGER.info("{} command initiated for cluster {}", KRAFT_MIGRATION_COMMAND_NAME, clusterName);

        try {
            Optional<ApiService> optionalKafkaService = findKafkaService(client, clusterName);

            if (optionalKafkaService.isPresent()) {
                executeOrRetryKraftMigrationCommand(client, optionalKafkaService.get(), stackDtoDelegate, KRAFT_MIGRATION_COMMAND_NAME);
            } else {
                LOGGER.error("Cannot migrate Zookeeper to KRaft. No {} service type found for cluster {}",
                        KAFKA_SERVICE_TYPE, clusterName);
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.error("Failed to migrate Zookeeper to KRaft", e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    public void rollbackZookeeperToKraftMigration(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        String clusterName = stackDtoDelegate.getCluster().getName();
        LOGGER.info("{} command initiated for cluster {}", KRAFT_ROLLBACK_MIGRATION_COMMAND_NAME, clusterName);

        try {
            Optional<ApiService> optionalKafkaService = findKafkaService(client, clusterName);

            if (optionalKafkaService.isPresent()) {
                executeOrRetryKraftMigrationCommand(client, optionalKafkaService.get(), stackDtoDelegate, KRAFT_ROLLBACK_MIGRATION_COMMAND_NAME);
            } else {
                LOGGER.error("Cannot rollback Zookeeper to KRaft migration. No {} service type found for cluster {}",
                        KAFKA_SERVICE_TYPE, clusterName);
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.error("Failed to rollback Zookeeper to KRaft migration", e);
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
                    "Timeout during waiting for command API to be available (Zookeeper to KRaft migration)");
        }
    }

    private void pollFinalizeZookeeperToKraftMigrationCommand(ApiClient client, StackDtoDelegate stackDtoDelegate, BigDecimal commandId)
            throws CloudbreakException {
        ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider
                .startPollingFinalizeZookeeperToKraftMigration(stackDtoDelegate, client, commandId);

        if (pollingResult.isExited()) {
            throw new CancellationException(
                    "Cluster was terminated while waiting for command API to be available for Zookeeper to KRaft migration finalization");
        } else if (pollingResult.isTimeout()) {
            throw new CloudbreakException(
                    "Timeout during waiting for command API to be available (Zookeeper to KRaft migration finalization)");
        }
    }

    private void pollRollbackZookeeperToKraftMigrationCommand(ApiClient client, StackDtoDelegate stackDtoDelegate, BigDecimal commandId)
            throws CloudbreakException {
        ExtendedPollingResult pollingResult = clouderaManagerPollingServiceProvider
                .startPollingRollbackZookeeperToKraftMigration(stackDtoDelegate, client, commandId);

        if (pollingResult.isExited()) {
            throw new CancellationException(
                    "Cluster was terminated while waiting for command API to be available for Zookeeper to KRaft migration rollback");
        } else if (pollingResult.isTimeout()) {
            throw new CloudbreakException(
                    "Timeout during waiting for command API to be available (Zookeeper to KRaft migration rollback)");
        }
    }

    private Collection<ApiService> readServices(ApiClient client, String clusterName) throws ApiException {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(client);
        return api.readServices(clusterName, ClouderaManagerConstants.SUMMARY).getItems();
    }

    private void executeOrRetryKraftMigrationCommand(ApiClient client, ApiService service, StackDtoDelegate stackDtoDelegate, String commandName)
            throws ApiException, CloudbreakException {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(client);
        String clusterName = stackDtoDelegate.getCluster().getName();
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
        Optional<BigDecimal> optionalLastKraftMigrationCommand = findLastCommandIdByCommandName(clustersResourceApi, stackDtoDelegate, commandName);
        ApiCommand kraftMigrationCommand;
        if (optionalLastKraftMigrationCommand.isPresent()) {
            LOGGER.debug("Previous {} command found for cluster {}", commandName, clusterName);
            BigDecimal lastKraftMigrationCommandId = optionalLastKraftMigrationCommand.get();
            ApiCommand lastKraftMigrationCommand = clouderaManagerCommandsService.getApiCommand(client, lastKraftMigrationCommandId);
            Boolean commandActive = lastKraftMigrationCommand.isActive();
            Boolean commandSuccess = lastKraftMigrationCommand.isSuccess();
            Boolean commandCanRetry = lastKraftMigrationCommand.isCanRetry();
            if (isTrue(commandActive)) {
                LOGGER.debug("{} is already running with id: [{}]", commandName, lastKraftMigrationCommandId);
            } else {
                if (isFalse(commandSuccess) && isTrue(commandCanRetry)) {
                    LOGGER.debug("Retrying last failed {} command with id {}", commandName, lastKraftMigrationCommandId);
                    BigDecimal retriedCommandId = clouderaManagerCommandsService.retryApiCommand(client, lastKraftMigrationCommandId).getId();
                    pollCommandByType(client, stackDtoDelegate, retriedCommandId, commandName);
                } else {
                    LOGGER.debug("Last {} command ({}) is not active, it was {} successful and {} retryable, submitting it now",
                            commandName,
                            lastKraftMigrationCommandId,
                            commandSuccess ? "" : "not",
                            commandCanRetry ? "" : "not");
                    kraftMigrationCommand = api.serviceCommandByName(clusterName, commandName, service.getName());
                    pollCommandByType(client, stackDtoDelegate, kraftMigrationCommand.getId(), commandName);
                }
            }
        } else {
            LOGGER.debug("Submitting new {} command for {} cluster", commandName, clusterName);
            kraftMigrationCommand = api.serviceCommandByName(clusterName, commandName, service.getName());
            pollCommandByType(client, stackDtoDelegate, kraftMigrationCommand.getId(), commandName);
        }
    }

    private Optional<BigDecimal> findLastCommandIdByCommandName(ClustersResourceApi clustersResourceApi, StackDtoDelegate stack, String commandName) {
        try {
            return syncApiCommandRetriever.getCommandId(commandName, clustersResourceApi, stack.getStack());
        } catch (CloudbreakException | ApiException e) {
            LOGGER.warn("Unexpected error during CM command table fetching, assuming no such command exists", e);
            return Optional.empty();
        }
    }

    private void pollCommandByType(ApiClient client, StackDtoDelegate stackDtoDelegate, BigDecimal commandId, String commandName)
            throws CloudbreakException {
        switch (commandName) {
            case KRAFT_MIGRATION_COMMAND_NAME:
                pollKraftMigrationCommand(client, stackDtoDelegate, commandId);
                break;
            case KRAFT_FINALIZE_MIGRATION_COMMAND_NAME:
                pollFinalizeZookeeperToKraftMigrationCommand(client, stackDtoDelegate, commandId);
                break;
            case KRAFT_ROLLBACK_MIGRATION_COMMAND_NAME:
                pollRollbackZookeeperToKraftMigrationCommand(client, stackDtoDelegate, commandId);
                break;
            default:
                throw new CloudbreakException("Unknown KRaft migration command: " + commandName);
        }
    }

    private Optional<ApiService> findKafkaService(ApiClient client, String clusterName) throws ApiException {
        Collection<ApiService> apiServices = readServices(client, clusterName);

        return apiServices.stream()
                .filter(service -> KAFKA_SERVICE_TYPE.equals(service.getType()))
                .findFirst();
    }
}