package com.sequenceiq.cloudbreak.cm;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

@Service
public class ClouderaManagerKraftMigrationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerKraftMigrationService.class);

    private static final String KAFKA_SERVICE_TYPE = "KAFKA";

    private static final String ZOOKEEPER_SERVICE_TYPE = "ZOOKEEPER";

    private static final String KRAFT_ROLE_FILTER = "type==KRAFT";

    private static final String KAFKA_BROKER_ROLE_FILTER = "type==KAFKA_BROKER";

    private static final String KRAFT_MIGRATION_COMMAND_NAME = "KRaftMigrationCommand";

    private static final String KRAFT_FINALIZE_MIGRATION_COMMAND_NAME = "KRaftFinalizeMigrationCommand";

    private static final String KRAFT_ROLLBACK_MIGRATION_COMMAND_NAME = "KRaftRollbackMigrationCommand";

    private static final String KRAFT_ROLE_TYPE = "KRAFT";

    private static final String TRANSIENT_KRAFT_PRECHECK_FAILURE_FRAGMENT = "unstable state";

    private static final Set<ApiRoleState> TRANSITIONING_ROLE_STATES = Set.of(ApiRoleState.STARTING, ApiRoleState.STOPPING, ApiRoleState.BUSY);

    private static final String KRAFT_PROPERTIES_ROLE_SAFETY_VALVE_KEY = "kraft.properties_role_safety_valve";

    private static final String KRAFT_PROPERTIES_ROLE_SAFETY_VALVE_VALUE = """
            zookeeper.metadata.migration.enable=true
            log.dirs=/hadoopfs/fs1/kraft""".stripIndent();

    private static final String METADATA_LOG_DIR_PROPERTY = "metadata.log.dir";

    private static final String METADATA_LOG_DIR_VALUE = "/hadoopfs/fs1/kraft";

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

    @Value("${cb.cm.kraft.migration.precheck.retry.count:2}")
    private int transientPrecheckRetryCount;

    // 60 attempts x 10s = a 10-minute ceiling: sized from the CM server logs in CB-33722 (the roles stayed unstable for ~2.5 min
    // after the mid-migration rollback race before the migration finalized) and aligned with the migration command's own 10-minute
    // poll budget (POLL_FOR_10_MINUTES) so the wait can outlast a full broker rolling restart before giving up.
    @Value("${cb.cm.kraft.migration.role.stabilization.attempts:60}")
    private int roleStabilizationMaxAttempts;

    @Value("${cb.cm.kraft.migration.role.stabilization.interval.millis:10000}")
    private long roleStabilizationPollIntervalMillis;

    public void configureZookeeperToKraftMigration(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        String clusterName = stackDtoDelegate.getCluster().getName();
        ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
        RoleConfigGroupsResourceApi roleConfigGroupsResourceApi = clouderaManagerApiFactory.getRoleConfigGroupsResourceApi(client);
        String serviceName = requireServiceName(clusterName, KAFKA_SERVICE_TYPE, servicesResourceApi);
        try {
            String roleConfigGroupName = configService.getRoleConfigGroupNameByTypeAndServiceName(KRAFT_ROLE_TYPE, clusterName, serviceName,
                    roleConfigGroupsResourceApi);
            configService.modifyRoleConfigGroup(client, clusterName, serviceName, roleConfigGroupName,
                    Map.of(KRAFT_PROPERTIES_ROLE_SAFETY_VALVE_KEY, KRAFT_PROPERTIES_ROLE_SAFETY_VALVE_VALUE,
                            METADATA_LOG_DIR_PROPERTY, METADATA_LOG_DIR_VALUE));
        } catch (ApiException e) {
            LOGGER.debug("Error when retrieving {} for service {} in cluster {}.", KRAFT_ROLE_TYPE, serviceName, clusterName, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    public void migrateZookeeperToKraft(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        executeKafkaServiceCommand(client, stackDtoDelegate, KraftCommand.MIGRATE);
    }

    public void finalizeZookeeperToKraftMigration(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        executeKafkaServiceCommand(client, stackDtoDelegate, KraftCommand.FINALIZE);
    }

    public void rollbackZookeeperToKraftMigration(ApiClient client, StackDtoDelegate stackDtoDelegate) {
        executeKafkaServiceCommand(client, stackDtoDelegate, KraftCommand.ROLLBACK);
    }

    private void executeKafkaServiceCommand(ApiClient client, StackDtoDelegate stackDtoDelegate, KraftCommand command) {
        String clusterName = stackDtoDelegate.getCluster().getName();
        LOGGER.info("{} command initiated for cluster {}", command.commandName(), clusterName);
        try {
            ServicesResourceApi servicesResourceApi = clouderaManagerApiFactory.getServicesResourceApi(client);
            String kafkaServiceName = requireServiceName(clusterName, KAFKA_SERVICE_TYPE, servicesResourceApi);
            executeKraftMigrationCommand(client, kafkaServiceName, stackDtoDelegate, command);
        } catch (ApiException | CloudbreakException e) {
            LOGGER.error("Failed to {}", command.operationDescription(), e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private void executeKraftMigrationCommand(ApiClient client, String kafkaServiceName, StackDtoDelegate stackDtoDelegate, KraftCommand command)
            throws ApiException, CloudbreakException {
        String clusterName = stackDtoDelegate.getCluster().getName();
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
        if (isLastCommandActive(client, clustersResourceApi, stackDtoDelegate, command.commandName())) {
            LOGGER.debug("{} is already running for cluster {}, skipping a new submission", command.commandName(), clusterName);
            return;
        }
        LOGGER.debug("Submitting new {} command for cluster {}", command.commandName(), clusterName);
        submitAndPollWithStabilityGuard(client, kafkaServiceName, stackDtoDelegate, command);
    }

    private boolean isLastCommandActive(ApiClient client, ClustersResourceApi clustersResourceApi, StackDtoDelegate stackDtoDelegate, String commandName)
            throws ApiException {
        Optional<Long> lastCommandId = findLastCommandIdByCommandName(clustersResourceApi, stackDtoDelegate, commandName);
        return lastCommandId.isPresent() && clouderaManagerCommandsService.getApiCommand(client, lastCommandId.get()).isActive();
    }

    /**
     * Submits the KRaft migration/rollback/finalize command and polls it to completion, guarding against transient CM prechecks.
     * <p>
     * Before every submission it waits for the Kafka/KRaft roles to leave any transitioning state and for any in-flight
     * {@code KRaftMigrationCommand} to finish (layer 1). If CM still rejects the command with a transient precheck error
     * ("unstable state"), it waits for the roles to stabilize again and retries up to {@link #transientPrecheckRetryCount}
     * times (layer 2). Non-transient failures (including config-staleness rejections), timeouts and cancellations propagate.
     */
    private void submitAndPollWithStabilityGuard(ApiClient client, String kafkaServiceName, StackDtoDelegate stackDtoDelegate, KraftCommand command)
            throws ApiException, CloudbreakException {
        String clusterName = stackDtoDelegate.getCluster().getName();
        ServicesResourceApi api = clouderaManagerApiFactory.getServicesResourceApi(client);
        int maxAttempts = 1 + Math.max(0, transientPrecheckRetryCount);
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            waitForKafkaRolesToStabilize(client, stackDtoDelegate, kafkaServiceName);
            ApiCommand newCommand = api.serviceCommandByName(clusterName, command.commandName(), kafkaServiceName);
            try {
                pollCommand(client, stackDtoDelegate, newCommand.getId(), command);
                return;
            } catch (ClouderaManagerOperationFailedException e) {
                if (attempt < maxAttempts && isTransientKraftPrecheckFailure(e)) {
                    LOGGER.warn("{} failed with a transient KRaft precheck error on attempt {}/{} for cluster {}. Waiting for the Kafka roles to "
                            + "stabilise and retrying. Cause: {}", command.commandName(), attempt, maxAttempts, clusterName, e.getMessage());
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * Waits until the Kafka {@code KRAFT} and {@code KAFKA_BROKER} roles are no longer transitioning and no
     * {@code KRaftMigrationCommand} is still running, so a migration/rollback command is not fired mid-transition.
     */
    private void waitForKafkaRolesToStabilize(ApiClient client, StackDtoDelegate stackDtoDelegate, String kafkaServiceName) throws ApiException {
        String clusterName = stackDtoDelegate.getCluster().getName();
        RolesResourceApi rolesResourceApi = clouderaManagerApiFactory.getRolesResourceApi(client);
        ClustersResourceApi clustersResourceApi = clouderaManagerApiFactory.getClustersResourceApi(client);
        for (int attempt = 1; attempt <= roleStabilizationMaxAttempts; attempt++) {
            if (kafkaRolesSettled(rolesResourceApi, clusterName, kafkaServiceName)
                    && noActiveKraftMigrationCommand(client, clustersResourceApi, stackDtoDelegate)) {
                return;
            }
            LOGGER.debug("Kafka KRaft/broker roles are not in a stable state yet for cluster {} (attempt {}/{}), waiting {} ms before rechecking.",
                    clusterName, attempt, roleStabilizationMaxAttempts, roleStabilizationPollIntervalMillis);
            sleepBetweenStabilizationChecks();
        }
        throw new ClouderaManagerOperationFailedException(String.format(
                "Kafka roles did not reach a stable state within the wait window for cluster %s", clusterName));
    }

    private boolean kafkaRolesSettled(RolesResourceApi rolesResourceApi, String clusterName, String kafkaServiceName) throws ApiException {
        return rolesInStableState(rolesResourceApi, clusterName, kafkaServiceName, KRAFT_ROLE_FILTER)
                && rolesInStableState(rolesResourceApi, clusterName, kafkaServiceName, KAFKA_BROKER_ROLE_FILTER);
    }

    private boolean rolesInStableState(RolesResourceApi rolesResourceApi, String clusterName, String serviceName, String filter) throws ApiException {
        return roleItems(rolesResourceApi, clusterName, serviceName, filter).stream()
                .map(ApiRole::getRoleState)
                .filter(Objects::nonNull)
                .noneMatch(TRANSITIONING_ROLE_STATES::contains);
    }

    private boolean noActiveKraftMigrationCommand(ApiClient client, ClustersResourceApi clustersResourceApi, StackDtoDelegate stackDtoDelegate)
            throws ApiException {
        return !isLastCommandActive(client, clustersResourceApi, stackDtoDelegate, KRAFT_MIGRATION_COMMAND_NAME);
    }

    /**
     * CM rejects a KRaft rollback when the KRaft roles are momentarily transitioning right after a preceding migration
     * ("There are KRaft roles with an unstable state...", CB-33722, e.g. a rollback fired mid-migration). That is a timing race
     * which clears once the roles settle, so it is worth waiting for stabilization and retrying. Other rejections (notably
     * genuine config-staleness) are not transient and must propagate.
     */
    private boolean isTransientKraftPrecheckFailure(ClouderaManagerOperationFailedException e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        return message.toLowerCase(Locale.ROOT).contains(TRANSIENT_KRAFT_PRECHECK_FAILURE_FRAGMENT);
    }

    private void sleepBetweenStabilizationChecks() {
        if (roleStabilizationPollIntervalMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(roleStabilizationPollIntervalMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ClouderaManagerOperationFailedException("Interrupted while waiting for Kafka roles to stabilize", e);
        }
    }

    private Optional<Long> findLastCommandIdByCommandName(ClustersResourceApi clustersResourceApi, StackDtoDelegate stack, String commandName) {
        try {
            return syncApiCommandRetriever.getCommandId(commandName, clustersResourceApi, stack.getStack());
        } catch (CloudbreakException | ApiException e) {
            LOGGER.warn("Unexpected error during CM command table fetching, assuming no such command exists", e);
            return Optional.empty();
        }
    }

    private void pollCommand(ApiClient client, StackDtoDelegate stackDtoDelegate, Long commandId, KraftCommand command) throws CloudbreakException {
        ExtendedPollingResult pollingResult = command.poll(clouderaManagerPollingServiceProvider, stackDtoDelegate, client, commandId);
        evaluatePollingResult(pollingResult, command.pollDescription());
    }

    private void evaluatePollingResult(ExtendedPollingResult pollingResult, String operationDescription) throws CloudbreakException {
        if (pollingResult.isExited()) {
            throw new CancellationException("Cluster was terminated while waiting for command API to be available for " + operationDescription);
        } else if (pollingResult.isTimeout()) {
            throw new CloudbreakException("Timeout during waiting for command API to be available (" + operationDescription + ")");
        }
    }

    /**
     * Prepares a cluster for ZooKeeper-to-KRaft migration when the stack has no dedicated KRaft host group.
     * <p>
     * For each host running a ZooKeeper role, this method ensures a matching {@code KRaft} role exists on the Kafka
     * service in Cloudera Manager. New roles are created in {@link ApiRoleState#STOPPED} state so controllers are
     * provisioned but not started; starting and migration are handled by later flow steps
     * ({@link #configureZookeeperToKraftMigration}, {@link #migrateZookeeperToKraft}, and related commands).
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

            List<ApiHostRef> zookeeperHosts = hostRefsFromRoles(rolesResourceApi, clusterName, zookeeperServiceName);
            LOGGER.debug("Found {} Zookeeper host(s) for cluster {}", zookeeperHosts.size(), clusterName);
            if (zookeeperHosts.isEmpty()) {
                LOGGER.debug("No Zookeeper hosts found, skipping KRaft role creation.");
                return;
            }

            Set<String> hostsWithKraftRole = kraftRoleHostnames(rolesResourceApi, clusterName, kafkaServiceName);
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
        } catch (ClouderaManagerOperationFailedException e) {
            LOGGER.warn("CM operation failed due to: {}", e.getMessage(), e);
            throw new CloudbreakException(e);
        } catch (ApiException e) {
            LOGGER.warn("Exception occurred during communicating with CM - {}", e.getMessage(), e);
            throw new CloudbreakException(e);
        }
    }

    private String requireServiceName(String clusterName, String serviceType, ServicesResourceApi servicesResourceApi) {
        try {
            LOGGER.debug("Looking for service of type {} in cluster {}", serviceType, clusterName);
            return servicesResourceApi.readServices(clusterName, DataView.SUMMARY.name()).getItems().stream()
                    .filter(service -> serviceType.equalsIgnoreCase(service.getType()))
                    .map(ApiService::getName)
                    .findFirst()
                    .orElseThrow(() -> new ClouderaManagerOperationFailedException(String.format("Service of type: %s is not found", serviceType)));
        } catch (ApiException e) {
            LOGGER.debug("Failed to get {} service name from Cloudera Manager.", serviceType, e);
            throw new ClouderaManagerOperationFailedException(e.getMessage(), e);
        }
    }

    private List<ApiHostRef> hostRefsFromRoles(RolesResourceApi rolesResourceApi, String clusterName, String serviceName)
            throws ApiException {
        return roleItems(rolesResourceApi, clusterName, serviceName, null).stream()
                .map(ApiRole::getHostRef)
                .filter(Objects::nonNull)
                .toList();
    }

    private Set<String> kraftRoleHostnames(RolesResourceApi rolesResourceApi, String clusterName, String serviceName)
            throws ApiException {
        return roleItems(rolesResourceApi, clusterName, serviceName, KRAFT_ROLE_FILTER).stream()
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

    @FunctionalInterface
    private interface PollingInvocation {
        ExtendedPollingResult poll(ClouderaManagerPollingServiceProvider provider, StackDtoDelegate stack, ApiClient client, Long commandId);
    }

    /**
     * The three ZooKeeper-to-KRaft service commands, each bundling its CM command name, the human-readable operation description used in logs,
     * the phrasing used in polling exception messages, and the polling-provider call that waits for it.
     */
    private enum KraftCommand {

        MIGRATE(KRAFT_MIGRATION_COMMAND_NAME, "migrate Zookeeper to KRaft", "Zookeeper to KRaft migration",
                ClouderaManagerPollingServiceProvider::startPollingZookeeperToKraftMigration),
        FINALIZE(KRAFT_FINALIZE_MIGRATION_COMMAND_NAME, "finalize Zookeeper to KRaft migration", "Zookeeper to KRaft migration finalization",
                ClouderaManagerPollingServiceProvider::startPollingFinalizeZookeeperToKraftMigration),
        ROLLBACK(KRAFT_ROLLBACK_MIGRATION_COMMAND_NAME, "rollback Zookeeper to KRaft migration", "Zookeeper to KRaft migration rollback",
                ClouderaManagerPollingServiceProvider::startPollingRollbackZookeeperToKraftMigration);

        private final String commandName;

        private final String operationDescription;

        private final String pollDescription;

        private final PollingInvocation pollingInvocation;

        KraftCommand(String commandName, String operationDescription, String pollDescription, PollingInvocation pollingInvocation) {
            this.commandName = commandName;
            this.operationDescription = operationDescription;
            this.pollDescription = pollDescription;
            this.pollingInvocation = pollingInvocation;
        }

        String commandName() {
            return commandName;
        }

        String operationDescription() {
            return operationDescription;
        }

        String pollDescription() {
            return pollDescription;
        }

        ExtendedPollingResult poll(ClouderaManagerPollingServiceProvider provider, StackDtoDelegate stack, ApiClient client, Long commandId) {
            return pollingInvocation.poll(provider, stack, client, commandId);
        }
    }
}