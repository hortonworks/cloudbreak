package com.sequenceiq.cloudbreak.cm;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.RoleConfigGroupsResourceApi;
import com.cloudera.api.swagger.RolesResourceApi;
import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRole;
import com.cloudera.api.swagger.model.ApiRoleList;
import com.cloudera.api.swagger.model.ApiRoleState;
import com.cloudera.api.swagger.model.ApiService;
import com.cloudera.api.swagger.model.ApiServiceRef;
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

    private static final String ZOOKEEPER_SERVICE_TYPE = "ZOOKEEPER";

    private static final String KRAFT_ROLE_FILTER = "type==KRAFT";

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

    public void migrateZookeeperToKraft(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        executeMigrationCommandOnKafkaService(client, stackDtoDelegate, KRAFT_MIGRATION_COMMAND_NAME, "migrate Zookeeper to KRaft");
    }

    public void finalizeZookeeperToKraftMigration(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        executeMigrationCommandOnKafkaService(client, stackDtoDelegate, KRAFT_FINALIZE_MIGRATION_COMMAND_NAME, "finalize Zookeeper to KRaft migration");
    }

    public void rollbackZookeeperToKraftMigration(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        executeMigrationCommandOnKafkaService(client, stackDtoDelegate, KRAFT_ROLLBACK_MIGRATION_COMMAND_NAME, "rollback Zookeeper to KRaft migration");
    }

    private void executeMigrationCommandOnKafkaService(ApiClient client, StackDtoDelegate stackDtoDelegate, String commandName,
            String operationDescription) {
        String clusterName = stackDtoDelegate.getCluster().getName();
        LOGGER.info("{} command initiated for cluster {}", commandName, clusterName);
        try {
            Optional<ApiService> optionalKafkaService = findKafkaService(client, clusterName);
            if (optionalKafkaService.isPresent()) {
                executeKraftMigrationCommand(client, optionalKafkaService.get(), stackDtoDelegate, commandName);
            } else {
                LOGGER.error("Cannot {}. No {} service type found for cluster {}", operationDescription, KAFKA_SERVICE_TYPE, clusterName);
            }
        } catch (ApiException | CloudbreakException e) {
            LOGGER.error("Failed to {}", operationDescription, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private Optional<ApiService> findKafkaService(ApiClient client, String clusterName) throws ApiException {
        Collection<ApiService> apiServices = readServices(client, clusterName);
        return apiServices.stream()
                .filter(service -> KAFKA_SERVICE_TYPE.equals(service.getType()))
                .findFirst();
    }

    private Collection<ApiService> readServices(ApiClient client, String clusterName) throws ApiException {
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(client);
        return api.readServices(clusterName, ClouderaManagerConstants.SUMMARY).getItems();
    }

    private void executeKraftMigrationCommand(ApiClient client, ApiService service, StackDtoDelegate stackDtoDelegate, String commandName)
            throws ApiException, CloudbreakException {
        String clusterName = stackDtoDelegate.getCluster().getName();
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
        Optional<Long> optionalLastCommandId = findLastCommandIdByCommandName(clustersResourceApi, stackDtoDelegate, commandName);
        if (optionalLastCommandId.isPresent()) {
            Long lastCommandId = optionalLastCommandId.get();
            LOGGER.debug("Previous {} command found for cluster {}", commandName, clusterName);
            if (clouderaManagerCommandsService.getApiCommand(client, lastCommandId).isActive()) {
                LOGGER.debug("{} is already running with id: [{}]", commandName, lastCommandId);
                return;
            }
            LOGGER.debug("Last {} command ({}) is not active, submitting a new one now", commandName, lastCommandId);
        } else {
            LOGGER.debug("Submitting new {} command for {} cluster", commandName, clusterName);
        }
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(client);
        ApiCommand newCommand = api.serviceCommandByName(clusterName, commandName, service.getName());
        pollCommandByType(client, stackDtoDelegate, newCommand.getId(), commandName);
    }

    private Optional<Long> findLastCommandIdByCommandName(ClustersResourceApi clustersResourceApi, StackDtoDelegate stack, String commandName) {
        try {
            return syncApiCommandRetriever.getCommandId(commandName, clustersResourceApi, stack.getStack());
        } catch (CloudbreakException | ApiException e) {
            LOGGER.warn("Unexpected error during CM command table fetching, assuming no such command exists", e);
            return Optional.empty();
        }
    }

    private void pollCommandByType(ApiClient client, StackDtoDelegate stackDtoDelegate, Long commandId, String commandName)
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

    private void pollKraftMigrationCommand(ApiClient client, StackDtoDelegate stackDtoDelegate, Long commandId)
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

    private void pollFinalizeZookeeperToKraftMigrationCommand(ApiClient client, StackDtoDelegate stackDtoDelegate, Long commandId)
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

    private void pollRollbackZookeeperToKraftMigrationCommand(ApiClient client, StackDtoDelegate stackDtoDelegate, Long commandId)
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

    /**
     * Prepares a cluster for ZooKeeper-to-KRaft migration when the stack has no dedicated KRaft host group.
     * <p>
     * For each host running a ZooKeeper role, this method ensures a matching {@code KRaft} role exists on the Kafka
     * service in Cloudera Manager. New roles are created in {@link ApiRoleState#STOPPED} state so controllers are
     * provisioned but not started; starting and migration are handled by later flow steps
     * ({@link #enableZookeeperMigrationMode}, {@link #migrateZookeeperToKraft}, and related commands).
     * <p>
     * The operation is idempotent: hosts that already have a KRaft role on Kafka are skipped, and the method returns
     * without calling CM when every Zookeeper host is already covered or when no Zookeeper roles are present.
     */
    public void installKraftAsStopped(ApiClient client, StackDtoDelegate stackDtoDelegate) throws CloudbreakException {
        String clusterName = stackDtoDelegate.getCluster().getName();
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);

        try {
            String kafkaServiceName = requireServiceName(clusterName, KAFKA_SERVICE_TYPE, servicesResourceApi);
            String zookeeperServiceName = requireServiceName(clusterName, ZOOKEEPER_SERVICE_TYPE, servicesResourceApi);

            List<ApiHostRef> zookeeperHosts = hostRefsFromRoles(rolesResourceApi, clusterName, zookeeperServiceName, null);
            LOGGER.debug("Found {} Zookeeper host(s) for cluster {}", zookeeperHosts.size(), clusterName);
            if (zookeeperHosts.isEmpty()) {
                LOGGER.debug("No Zookeeper hosts found, skipping KRaft role creation.");
                return;
            }

            Set<String> hostsWithKraftRole = hostnamesFromRoles(rolesResourceApi, clusterName, kafkaServiceName, KRAFT_ROLE_FILTER);
            List<ApiHostRef> hostsNeedingKraftRole = zookeeperHosts.stream()
                    .filter(host -> host.getHostname() != null && !hostsWithKraftRole.contains(host.getHostname()))
                    .toList();
            if (hostsNeedingKraftRole.isEmpty()) {
                LOGGER.debug("KRaft roles already exist on all required host(s), skipping role creation.");
                return;
            }

            ApiRoleList kraftRoleList = buildStoppedKraftRoles(clusterName, kafkaServiceName, hostsNeedingKraftRole);
            LOGGER.info("Creating stopped KRaft role(s) on Kafka service {} for host(s): {}",
                    kafkaServiceName, hostsNeedingKraftRole.stream().map(ApiHostRef::getHostname).toList());
            rolesResourceApi.createRoles(clusterName, kafkaServiceName, kraftRoleList);
        } catch (ClouderaManagerOperationFailedException cmOpFailedExc) {
            LOGGER.warn("CM operation failed due to: {}", cmOpFailedExc.getMessage(), cmOpFailedExc);
            throw new CloudbreakException(cmOpFailedExc);
        } catch (ApiException e) {
            LOGGER.warn("Exception occurred during communicating with CM - {}", e.getMessage(), e);
            throw new CloudbreakException(e);
        }
    }

    private String requireServiceName(String clusterName, String serviceType, ServicesResourceApi servicesResourceApi) {
        return configService.getServiceName(clusterName, serviceType, servicesResourceApi)
                .orElseThrow(() -> new ClouderaManagerOperationFailedException(String.format("Service of type: %s is not found", serviceType)));
    }

    private List<ApiHostRef> hostRefsFromRoles(RolesResourceApi rolesResourceApi, String clusterName, String serviceName, String filter)
            throws ApiException {
        return roleItems(rolesResourceApi, clusterName, serviceName, filter).stream()
                .map(ApiRole::getHostRef)
                .filter(Objects::nonNull)
                .toList();
    }

    private Set<String> hostnamesFromRoles(RolesResourceApi rolesResourceApi, String clusterName, String serviceName, String filter)
            throws ApiException {
        return roleItems(rolesResourceApi, clusterName, serviceName, filter).stream()
                .map(ApiRole::getHostRef)
                .filter(Objects::nonNull)
                .map(ApiHostRef::getHostname)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private List<ApiRole> roleItems(RolesResourceApi rolesResourceApi, String clusterName, String serviceName, String filter) throws ApiException {
        return Optional.ofNullable(rolesResourceApi.readRoles(clusterName, serviceName, filter, DataView.FULL.name()).getItems())
                .orElse(List.of());
    }

    private ApiRoleList buildStoppedKraftRoles(String clusterName, String kafkaServiceName, List<ApiHostRef> hosts) {
        ApiRoleList kraftRoleList = new ApiRoleList();
        hosts.forEach(host -> kraftRoleList.addItemsItem(new ApiRole()
                .type(KRAFT_ROLE_TYPE)
                .roleState(ApiRoleState.STOPPED)
                .hostRef(host)
                .serviceRef(new ApiServiceRef()
                        .clusterName(clusterName)
                        .serviceName(kafkaServiceName))));
        return kraftRoleList;
    }
}