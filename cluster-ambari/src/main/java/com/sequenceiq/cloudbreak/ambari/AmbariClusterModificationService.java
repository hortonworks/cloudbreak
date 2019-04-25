package com.sequenceiq.cloudbreak.ambari;

import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_INSTALL_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_INIT_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STARTING;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_START_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOPPED;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOPPING;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_SERVICES_STOP_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariMessages.AMBARI_CLUSTER_UPSCALE_FAILED;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.INIT_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.INSTALL_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.START_SERVICES_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.STOP_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.AmbariOperationType.UPSCALE_AMBARI_PROGRESS_STATE;
import static com.sequenceiq.cloudbreak.ambari.HostGroupAssociationBuilder.FQDN;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isExited;
import static com.sequenceiq.cloudbreak.polling.PollingResult.isTimeout;
import static java.util.Collections.singletonMap;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.ambari.client.AmbariConnectionException;
import com.sequenceiq.cloudbreak.ambari.flow.AmbariOperationService;
import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterConnectorPollingResultChecker;
import com.sequenceiq.cloudbreak.cluster.service.ClusterException;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.retry.RetryUtil;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionWentFailException;
import com.sequenceiq.cloudbreak.service.event.CloudbreakEventService;

import groovyx.net.http.HttpResponseException;

@Service
@Scope("prototype")
public class AmbariClusterModificationService implements ClusterModificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariClusterModificationService.class);

    private static final String STATE_INSTALLED = "INSTALLED";

    private final Stack stack;

    private final HttpClientConfig clientConfig;

    @Inject
    private AmbariClientFactory ambariClientFactory;

    @Inject
    private ClusterConnectorPollingResultChecker clusterConnectorPollingResultChecker;

    @Inject
    private AmbariOperationService ambariOperationService;

    @Inject
    private HostGroupAssociationBuilder hostGroupAssociationBuilder;

    @Inject
    private CloudbreakMessagesService cloudbreakMessagesService;

    @Inject
    private CloudbreakEventService eventService;

    @Inject
    private AmbariPollingServiceProvider ambariPollingServiceProvider;

    @Inject
    private Retry retry;

    @Inject
    private AmbariRepositoryVersionService ambariRepositoryVersionService;

    private AmbariClient ambariClient;

    public AmbariClusterModificationService(Stack stack, HttpClientConfig clientConfig) {
        this.stack = stack;
        this.clientConfig = clientConfig;
    }

    @PostConstruct
    public void initAmbariClient() {
        ambariClient = ambariClientFactory.getAmbariClient(stack, stack.getCluster(), clientConfig);
    }

    @Override
    public void upscaleCluster(HostGroup hostGroup, Collection<HostMetadata> hostMetadata, List<InstanceMetaData> instanceMetaData)
            throws CloudbreakException {
        List<String> upscaleHostNames = hostMetadata
                .stream()
                .map(HostMetadata::getHostName)
                .filter(hostName -> !ambariClient.getClusterHosts().contains(hostName))
                .collect(Collectors.toList());
        if (!upscaleHostNames.isEmpty()) {
            Pair<PollingResult, Exception> pollingResult = ambariOperationService.waitForOperations(
                    stack,
                    ambariClient,
                    installServices(upscaleHostNames, stack, ambariClient, hostGroup, instanceMetaData),
                    UPSCALE_AMBARI_PROGRESS_STATE);
            String message = pollingResult.getRight() == null
                    ? cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_UPSCALE_FAILED.code())
                    : pollingResult.getRight().getMessage();
            clusterConnectorPollingResultChecker.checkPollingResult(pollingResult.getLeft(), message);
        }
    }

    @Override
    public void stopCluster() throws CloudbreakException {
        try {
            if (!isClusterStopped(ambariClient)) {
                try {
                    stopHadoopServices(stack, ambariClient);
                } catch (IOException | URISyntaxException e) {
                    throw new CloudbreakException("Failed to stop Hadoop services.", e);
                }
            }
        } catch (AmbariConnectionException ignored) {
            LOGGER.debug("Ambari not running on the gateway machine, no need to stop it.");
        }
    }

    private boolean isClusterStopped(AmbariClient ambariClient) {
        Collection<Map<String, String>> values = ambariClient.getHostComponentsStates().values();
        for (Map<String, String> value : values) {
            for (String state : value.values()) {
                if (!"INSTALLED".equals(state)) {
                    return false;
                }
            }
        }
        return true;
    }

    private void stopHadoopServices(Stack stack, AmbariClient ambariClient) throws URISyntaxException, IOException, CloudbreakException {
        LOGGER.info("Stop all Hadoop services");
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                        cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STOPPING.code()));
        int requestId = ambariClient.stopAllServices();
        if (requestId != -1) {
            waitForServicesToStop(stack, ambariClient, requestId);
        } else {
            LOGGER.warn("Failed to stop Hadoop services.");
            throw new CloudbreakException("Failed to stop Hadoop services.");
        }
        eventService
                .fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                        cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STOPPED.code()));
    }

    private void waitForServicesToStop(Stack stack, AmbariClient ambariClient, int requestId) throws CloudbreakException {
        LOGGER.info("Waiting for Hadoop services to stop on stack");
        PollingResult servicesStopResult = ambariOperationService.waitForOperations(stack, ambariClient, singletonMap("stop services", requestId),
                STOP_AMBARI_PROGRESS_STATE).getLeft();
        if (isExited(servicesStopResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Hadoop services to start");
        } else if (isTimeout(servicesStopResult)) {
            throw new CloudbreakException("Timeout while stopping Ambari services.");
        }
    }

    @Override
    public int startCluster(Set<HostMetadata> hostsInCluster) throws CloudbreakException {
        PollingResult ambariHealthCheckResult = ambariPollingServiceProvider.ambariHealthChecker(stack, ambariClient);
        if (isExited(ambariHealthCheckResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Ambari to start.");
        } else if (isTimeout(ambariHealthCheckResult)) {
            throw new CloudbreakException("Ambari server was not restarted properly.");
        }
        waitForAmbariHosts(stack, ambariClient, hostsInCluster);
        try {
            return startHadoopServices(stack, ambariClient);
        } catch (IOException | URISyntaxException e) {
            throw new CloudbreakException("Starting Hadoop services failed.", e);
        }
    }

    private void waitForAmbariHosts(Stack stack, AmbariClient ambariClient, Set<HostMetadata> hostsInCluster) {
        LOGGER.info("Starting Ambari agents on the hosts.");
        PollingResult hostsJoinedResult = ambariPollingServiceProvider.ambariHostJoin(stack, ambariClient, hostsInCluster);
        if (isExited(hostsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while starting Ambari agents.");
        }
    }

    private void waitForComponents(Stack stack, AmbariClient ambariClient, Set<HostMetadata> hostsInCluster) {
        PollingResult componentsJoinedResult = ambariPollingServiceProvider.ambariComponentJoin(stack, ambariClient, hostsInCluster);
        if (isExited(componentsJoinedResult)) {
            throw new CancellationException("Cluster was terminated while waiting for Hadoop components to join.");
        }
    }

    private int startHadoopServices(Stack stack, AmbariClient ambariClient) throws CloudbreakException, IOException, URISyntaxException {
        LOGGER.info("Starting all Hadoop services");
        eventService.fireCloudbreakEvent(stack.getId(), UPDATE_IN_PROGRESS.name(),
                cloudbreakMessagesService.getMessage(AMBARI_CLUSTER_SERVICES_STARTING.code()));
        int requestId = ambariClient.startAllServices();
        if (requestId == -1) {
            LOGGER.error("Failed to start Hadoop services.");
            throw new CloudbreakException("Failed to start Hadoop services.");
        }
        return requestId;
    }

    @Override
    public Map<String, String> gatherInstalledComponents(String hostname) {
        Set<String> components = ambariClient.getHostComponentsMap(hostname).keySet();
        return ambariClient.getComponentsCategory(new ArrayList<>(components));
    }

    @Override
    public void stopComponents(Map<String, String> components, String hostname) throws CloudbreakException {
        stopComponentsInternal(stack, new ArrayList<>(components.keySet()), hostname, OperationParameters.DO_NOT_WAIT);
    }

    @Override
    public void ensureComponentsAreStopped(Map<String, String> components, String hostname) throws CloudbreakException {
        Map<String, String> masterSlaveComponentsWithState = getMasterSlaveComponentStatuses(components, hostname, ambariClient);
        Map<String, String> componentsNotInDesiredState = masterSlaveComponentsWithState.entrySet().stream()
                .filter(e -> !STATE_INSTALLED.equals(e.getValue()))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        if (!componentsNotInDesiredState.isEmpty()) {
            LOGGER.info("Some components are not in {} state: {}, stopping them",
                    STATE_INSTALLED, componentsNotInDesiredStateToString(componentsNotInDesiredState));
            stopComponentsInternal(stack, new ArrayList<>(componentsNotInDesiredState.keySet()), hostname,
                    new OperationParameters(STOP_AMBARI_PROGRESS_STATE, AMBARI_CLUSTER_SERVICES_STOP_FAILED));
        }
    }

    @Override
    public void initComponents(Map<String, String> components, String hostname) throws CloudbreakException {
        try {
            Map<String, Integer> operationRequests = ambariClient.initComponentsOnHost(hostname, collectMasterSlaveComponents(components));
            waitForOperation(stack, ambariClient, operationRequests, INIT_SERVICES_AMBARI_PROGRESS_STATE, AMBARI_CLUSTER_SERVICES_INIT_FAILED);
        } catch (RuntimeException | URISyntaxException | IOException e) {
            throw new CloudbreakException("Failed to init Hadoop services.", e);
        }
    }

    /**
     * Note: on ambari stopping and installing components is the same desired state: INSTALLED. If you want to install or resintall a component, you have to
     * 1) INSTALLED (if it is in STARTED or STARTING state
     * 2) INIT
     * 3) INSTALLED
     *
     * @param components Map of components - componentType (MASTER, SLAVE, CLIENT)
     * @param hostname   The host of ambari
     * @throws CloudbreakException thrown in case of any exception
     */
    @Override
    public void installComponents(Map<String, String> components, String hostname) throws CloudbreakException {
        stopComponentsInternal(stack, collectMasterSlaveComponents(components), hostname,
                new OperationParameters(INSTALL_SERVICES_AMBARI_PROGRESS_STATE, AMBARI_CLUSTER_INSTALL_FAILED));
    }

    @Override
    public void regenerateKerberosKeytabs(String hostname) throws CloudbreakException {
        try {
            KerberosConfig kerberosConfig = stack.getCluster().getKerberosConfig();
            LOGGER.info("Setting kerberos principal {} and password on master node {} ", kerberosConfig.getPrincipal(), hostname);
            ambariClient.setKerberosPrincipal(kerberosConfig.getPrincipal(), kerberosConfig.getPassword());
            LOGGER.info("Regenerating kerberos keytabs for missing nodes and services");
            Integer ambariTaskId = ambariClient.generateKeytabs(false);
            waitForOperation(stack, ambariClient, Map.of("KerberosRegenerateKeytabs", ambariTaskId), START_SERVICES_AMBARI_PROGRESS_STATE,
                    AMBARI_CLUSTER_SERVICES_START_FAILED);
        } catch (ClusterException | URISyntaxException | IOException e) {
            throw new CloudbreakException("Error regenerating keytabs on ambari", e);
        }
    }

    @Override
    public void startComponents(Map<String, String> components, String hostname) throws CloudbreakException {
        tryWithRetry(() -> {
            try {
                Map<String, Integer> operationRequests = ambariClient.startComponentsOnHost(hostname, collectMasterSlaveComponents(components));
                waitForOperation(stack, ambariClient, operationRequests, START_SERVICES_AMBARI_PROGRESS_STATE, AMBARI_CLUSTER_SERVICES_START_FAILED);
            } catch (RuntimeException | URISyntaxException | IOException e) {
                LOGGER.error("Error starting components on ambari", e);
                throw new RecoverableAmbariException(e);
            } catch (ClusterException e) {
                LOGGER.error("Error starting components on ambari", e);
                if (PollingResult.isFailure(e.getPollingResult())) {
                    throw new RecoverableAmbariException(e);
                }
                throw new IrrecoverableAmbariException(e);
            }
        });
    }

    @Override
    public void restartAll() throws CloudbreakException {
        tryWithRetry(() -> {
            try {
                Integer operationId = ambariClient.restartAllServices(stack.getCluster().getName());
                Map<String, Integer> operationRequests = Map.of("restartAllServices", operationId);
                waitForOperation(stack, ambariClient, operationRequests, START_SERVICES_AMBARI_PROGRESS_STATE, AMBARI_CLUSTER_SERVICES_START_FAILED);
            } catch (RuntimeException | URISyntaxException | IOException e) {
                LOGGER.error("Error starting components on ambari", e);
                throw new RecoverableAmbariException(e);
            } catch (ClusterException e) {
                LOGGER.error("Error starting components on ambari", e);
                if (PollingResult.isFailure(e.getPollingResult())) {
                    throw new RecoverableAmbariException(e);
                }
                throw new IrrecoverableAmbariException(e);
            }
        });
    }

    private Map<String, String> getMasterSlaveComponentStatuses(Map<String, String> components, String hostname, AmbariClient ambariClient)
            throws CloudbreakException {
        try {
            return retry.testWith2SecDelayMax15Times(() -> {
                Map<String, String> componentStatus = ambariClient.getHostComponentsMap(hostname);
                Map<String, String> masterSlaveWithState = collectMasterSlaveComponents(components).stream()
                        .collect(Collectors.toMap(Function.identity(), componentStatus::get));
                if (masterSlaveWithState.values().stream().anyMatch("UNKNOWN"::equals)) {
                    throw new ActionWentFailException("Ambari has not recovered");
                }
                return componentStatus;
            });
        } catch (ActionWentFailException e) {
            throw new CloudbreakException("Status of one or more components in ambari remained in UNKNOWN status.");
        }
    }

    private String componentsNotInDesiredStateToString(Map<String, String> componentsNotInDesiredState) {
        return componentsNotInDesiredState.entrySet().stream()
                .map(e -> String.format("[%s=>%s]", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", "));
    }

    private List<String> collectMasterSlaveComponents(Map<String, String> components) {
        List<String> serviceCategories = Arrays.asList("MASTER", "SLAVE");
        return components.entrySet().stream()
                .filter(e -> serviceCategories.contains(e.getValue()))
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    private void stopComponentsInternal(Stack stack, List<String> components, String hostname, OperationParameters operationParameters)
            throws CloudbreakException {
        tryWithRetry(() -> {
            try {
                Map<String, Integer> operationRequests = ambariClient.stopComponentsOnHost(hostname, components);
                if (operationParameters.waitForOperation) {
                    waitForOperation(stack, ambariClient, operationRequests, operationParameters.getAmbariOperationType(),
                            operationParameters.getOperationFailedMessage());
                }
            } catch (RuntimeException | URISyntaxException | IOException e) {
                LOGGER.error("Error stopping components on ambari", e);
                throw new RecoverableAmbariException(e);
            } catch (ClusterException e) {
                LOGGER.error("Error stopping components on ambari", e);
                if (PollingResult.isFailure(e.getPollingResult())) {
                    throw new RecoverableAmbariException(e);
                }
                throw new IrrecoverableAmbariException(e);
            }
        });
    }

    private void waitForOperation(Stack stack, AmbariClient ambariClient, Map<String, Integer> operationRequests,
            AmbariOperationType type, AmbariMessages failureMessage) throws ClusterException {
        operationRequests = operationRequests.entrySet().stream()
                .filter(e -> e.getValue() != -1)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        if (operationRequests.isEmpty()) {
            return;
        }
        Pair<PollingResult, Exception> pollingResult = ambariOperationService.waitForOperations(stack, ambariClient, operationRequests, type);
        String message = pollingResult.getRight() == null
                ? cloudbreakMessagesService.getMessage(failureMessage.code())
                : pollingResult.getRight().getMessage();
        clusterConnectorPollingResultChecker.checkPollingResult(pollingResult.getLeft(), message);
    }

    private void tryWithRetry(Runnable action) throws CloudbreakException {
        Queue<CloudbreakException> errors = new ArrayDeque<>();
        RetryUtil.withDefaultRetries()
                .retry(action::run)
                .checkIfRecoverable(e -> e instanceof RecoverableAmbariException)
                .ifNotRecoverable(e -> errors.add(new CloudbreakException(e)))
                .run();

        if (!errors.isEmpty()) {
            throw errors.poll();
        }
    }

    @Override
    public Map<String, String> getComponentsByCategory(String blueprintName, String hostGroupName) {
        return ambariClient.getComponentsCategory(blueprintName, hostGroupName);
    }

    private Map<String, Integer> installServices(List<String> hosts, Stack stack, AmbariClient ambariClient, HostGroup hostGroup,
            List<InstanceMetaData> metas) {
        try {
            String blueprintName = stack.getCluster().getBlueprint().getStackName();
            // In case If we changed the blueprintName field we need to query the validation name information from ambari
            Map<String, String> blueprintsMap = ambariClient.getBlueprintsMap();
            if (!blueprintsMap.entrySet().isEmpty()) {
                blueprintName = blueprintsMap.keySet().iterator().next();
            }
            List<Map<String, String>> hostGroupAssociation = hostGroupAssociationBuilder.buildHostGroupAssociation(hostGroup, metas);
            Map<String, String> hostsWithRackInfo = hostGroupAssociation.stream()
                    .filter(associationMap -> hosts.stream().anyMatch(host -> host.equals(associationMap.get(FQDN))))
                    .collect(Collectors.toMap(association -> association.get(FQDN), association ->
                            association.get("rack") != null ? association.get("rack") : "/default-rack"));
            int upscaleRequestCode = ambariClient.addHostsAndRackInfoWithBlueprint(blueprintName, hostGroup.getName(), hostsWithRackInfo);
            return singletonMap("UPSCALE_REQUEST", upscaleRequestCode);
        } catch (HttpResponseException e) {
            if ("Conflict".equals(e.getMessage())) {
                throw new AmbariServiceException("Host already exists.", e);
            } else {
                String errorMessage = AmbariClientExceptionUtil.getErrorMessage(e);
                throw new CloudbreakServiceException("Ambari could not install services. " + errorMessage, e);
            }
        } catch (IOException | URISyntaxException e) {
            throw new CloudbreakServiceException("Ambari could not install services. " + e.getMessage(), e);
        }
    }

    @Override
    public String getStackRepositoryJson(StackRepoDetails repoDetails, String stackRepoId) {
        try {
            String osType = ambariRepositoryVersionService.getOsTypeForStackRepoDetails(repoDetails);
            if (osType != null && osType.isEmpty()) {
                LOGGER.debug(String.format("The stored HDP repo details (%s) do not contain OS information for stack '%s'.", repoDetails, stack.getName()));
                return null;
            }

            String stackRepositoryJson = ambariClient.getLatestStackRepositoryAsJson(stack.getCluster().getName(), osType, stackRepoId);
            if (stackRepositoryJson == null) {
                throw new AmbariServiceException(String.format("Stack Repository response coming from Ambari server was null "
                        + "for cluster '%s' and repo url '%s'.", stack.getCluster().getName(), stackRepoId));
            }
            return stackRepositoryJson;
        } catch (AmbariConnectionException e) {
            if ("Not Found".equals(e.getMessage())) {
                throw new AmbariNotFoundException("Ambari validation not found.", e);
            } else {
                throw new CloudbreakServiceException("Could not get Stack Repository from Ambari as JSON: " + e.getMessage(), e);
            }
        }
    }

    private static class RecoverableAmbariException extends RuntimeException {
        RecoverableAmbariException(Exception cause) {
            super(cause);
        }
    }

    private static class IrrecoverableAmbariException extends RuntimeException {
        IrrecoverableAmbariException(Exception cause) {
            super(cause);
        }
    }

    private static class OperationParameters {

        private static final OperationParameters DO_NOT_WAIT = new OperationParameters();

        private final boolean waitForOperation;

        private AmbariOperationType ambariOperationType;

        private AmbariMessages operationFailedMessage;

        private OperationParameters(AmbariOperationType ambariOperationType, AmbariMessages operationFailedMessage) {
            waitForOperation = true;
            this.ambariOperationType = ambariOperationType;
            this.operationFailedMessage = operationFailedMessage;
        }

        private OperationParameters() {
            waitForOperation = false;
        }

        private boolean isWaitForOperation() {
            return waitForOperation;
        }

        private AmbariOperationType getAmbariOperationType() {
            return ambariOperationType;
        }

        private AmbariMessages getOperationFailedMessage() {
            return operationFailedMessage;
        }
    }
}
