package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static com.sequenceiq.cloudbreak.util.TimeUtil.MILLISEC_MULTIPLIER;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.retry.Retry;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.model.IpaServer;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.orchestrator.OrchestratorParamsProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class VerifyReplicationCleanupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VerifyReplicationCleanupService.class);

    private static final String CLEANUP_SALT_STATE = "freeipa/verify-replication-cleanup";

    private static final String CLEANUP_SALT_STATE_SLS = "freeipa.verify-replication-cleanup";

    private static final String CONVERGENCE_SALT_STATE = "freeipa/verify-replication-convergence";

    private static final String CONVERGENCE_SALT_STATE_SLS = "freeipa.verify-replication-convergence";

    private static final String CLEANUP_LOG = "/var/log/freeipa_replication_cleanup.log";

    private static final String CONVERGENCE_LOG = "/var/log/freeipa_replication_convergence.log";

    private static final int SLS_EXISTS_CONNECT_TIMEOUT_MS = 5_000;

    private static final int SLS_EXISTS_READ_TIMEOUT_MS = 15_000;

    private static final int POLL_INTERVAL_SEC = 10;

    private static final int POLL_MARGIN = 12;

    private static final int MAX_RETRY_ON_ERROR = 2;

    private static final String CLEANUP_TIMEOUT_MARKER = "TIMEOUT after";

    private static final String CONVERGENCE_TIMEOUT_MARKER = "not healthy within";

    @Value("${freeipa.replication.cleanup.polling.timeout-sec}")
    private long timeoutSec;

    @Value("${freeipa.replication.cleanup.polling.interval-sec}")
    private long intervalSec;

    @Value("${freeipa.replication.convergence.enabled}")
    private boolean convergenceEnabled;

    @Value("${freeipa.replication.convergence.timeout-sec}")
    private long convergenceTimeoutSec;

    @Value("${freeipa.replication.convergence.interval-sec}")
    private long convergenceIntervalSec;

    @Inject
    private OrchestratorParamsProvider orchestratorParamsProvider;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private CloudbreakMessagesService messagesService;

    @Inject
    private Retry retryService;

    public void verifyOnSurvivingMasters(Long stackId, Collection<String> removedHosts) throws CloudbreakOrchestratorException {
        Collection<String> hosts = removedHosts == null ? List.of() : removedHosts;
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        if (!hosts.isEmpty()) {
            verifyCleanupOnSurvivingMasters(stack, hosts);
        }
        verifyConvergenceOnSurvivingMasters(stack, hosts);
    }

    /**
     * Observe-only gate: fails when the 389-ds replication agreements of the given master are not healthy on both suffixes and in both
     * directions within the convergence timeout. Never restarts or repairs anything; the failure is meant to fail the flow step.
     */
    public void verifyConvergenceOnMaster(Stack stack, String masterFqdn) throws CloudbreakOrchestratorException {
        Function<CloudbreakOrchestratorException, String> executionError = e -> messagesService.getMessageWithArgs(
                ResourceEvent.FREEIPA_REPLICATION_CONVERGENCE_EXECUTION_FAILED_ON_MASTER.getMessage(), masterFqdn, e.getMessage());
        if (convergenceCheckEnabled(stack, executionError)) {
            OrchestratorStateParams stateParams = orchestratorParamsProvider.createStateParamsForSingleTarget(stack, masterFqdn,
                    CONVERGENCE_SALT_STATE);
            runConvergenceState(stack, stateParams, List.of(), "FreeIPA master " + masterFqdn,
                    () -> messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CONVERGENCE_FAILED_ON_MASTER.getMessage(),
                            String.valueOf(convergenceTimeoutSec), masterFqdn, CONVERGENCE_LOG),
                    executionError);
        }
    }

    private void verifyCleanupOnSurvivingMasters(Stack stack, Collection<String> removedHosts) throws CloudbreakOrchestratorException {
        if (verificationStateMissing(stack, CLEANUP_SALT_STATE_SLS, e -> buildCleanupExecutionError(removedHosts, e))) {
            LOGGER.info("Skipping replication cleanup verification for stack {} — salt state '{}' is not present on this cluster "
                    + "(it predates the feature)", stack.getResourceCrn(), CLEANUP_SALT_STATE_SLS);
        } else {
            performOptionalPreCheck(stack, removedHosts);
            LOGGER.info("Running replication cleanup verification on {} surviving master(s) for stack {}, removed hosts: {}",
                    stack.getNotDeletedInstanceMetaDataSet().size(), stack.getResourceCrn(), removedHosts);
            OrchestratorStateParams stateParams = orchestratorParamsProvider.createStateParams(stack.getId(), CLEANUP_SALT_STATE);
            stateParams.setStateParams(Map.of("freeipa", Map.of("replication_cleanup", Map.<String, Object>of(
                    "removed_hosts", String.join(",", removedHosts),
                    "timeout_sec", timeoutSec,
                    "interval_sec", intervalSec))));
            stateParams.setStateRetryParams(buildRetryParams(timeoutSec));
            try {
                hostOrchestrator.runOrchestratorState(stateParams);
                LOGGER.info("Replication cleanup verification completed successfully for stack {}", stack.getResourceCrn());
            } catch (CloudbreakOrchestratorException e) {
                boolean notConverged = scriptTimedOut(e, CLEANUP_TIMEOUT_MARKER);
                LOGGER.error("Replication cleanup verification failed for stack {}, removed hosts: {}, cleanup did not converge: {}",
                        stack.getResourceCrn(), removedHosts, notConverged, e);
                String message = notConverged ? buildCleanupUserFacingError(removedHosts) : buildCleanupExecutionError(removedHosts, e);
                throw new CloudbreakOrchestratorFailedException(message, e);
            }
        }
    }

    private void verifyConvergenceOnSurvivingMasters(Stack stack, Collection<String> removedHosts) throws CloudbreakOrchestratorException {
        Function<CloudbreakOrchestratorException, String> executionError = e -> messagesService.getMessageWithArgs(
                ResourceEvent.FREEIPA_REPLICATION_CONVERGENCE_EXECUTION_FAILED_ON_SURVIVING_MASTERS.getMessage(), e.getMessage());
        if (convergenceCheckEnabled(stack, executionError)) {
            OrchestratorStateParams stateParams = orchestratorParamsProvider.createStateParams(stack.getId(), CONVERGENCE_SALT_STATE);
            String target = String.format("%d surviving master(s), excluded removed hosts: %s", stack.getNotDeletedInstanceMetaDataSet().size(),
                    removedHosts);
            runConvergenceState(stack, stateParams, removedHosts, target,
                    () -> messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CONVERGENCE_FAILED_ON_SURVIVING_MASTERS.getMessage(),
                            String.valueOf(convergenceTimeoutSec), CONVERGENCE_LOG),
                    executionError);
        }
    }

    private void runConvergenceState(Stack stack, OrchestratorStateParams stateParams, Collection<String> excludedHosts, String target,
            Supplier<String> notConvergedError, Function<CloudbreakOrchestratorException, String> executionError)
            throws CloudbreakOrchestratorException {
        LOGGER.info("Running replication convergence verification on {} for stack {}", target, stack.getResourceCrn());
        stateParams.setStateParams(buildConvergencePillar(excludedHosts));
        stateParams.setStateRetryParams(buildRetryParams(convergenceTimeoutSec));
        try {
            hostOrchestrator.runOrchestratorState(stateParams);
            LOGGER.info("Replication convergence verification completed successfully on {} for stack {}", target, stack.getResourceCrn());
        } catch (CloudbreakOrchestratorException e) {
            boolean notConverged = scriptTimedOut(e, CONVERGENCE_TIMEOUT_MARKER);
            LOGGER.error("Replication convergence verification failed on {} for stack {}, replication did not converge: {}", target,
                    stack.getResourceCrn(), notConverged, e);
            String message = notConverged ? notConvergedError.get() : executionError.apply(e);
            throw new CloudbreakOrchestratorFailedException(message, e);
        }
    }

    private boolean convergenceCheckEnabled(Stack stack, Function<CloudbreakOrchestratorException, String> executionError)
            throws CloudbreakOrchestratorFailedException {
        if (!convergenceEnabled) {
            LOGGER.info("Skipping replication convergence verification for stack {} — disabled by freeipa.replication.convergence.enabled",
                    stack.getResourceCrn());
            return false;
        } else if (verificationStateMissing(stack, CONVERGENCE_SALT_STATE_SLS, executionError)) {
            LOGGER.info("Skipping replication convergence verification for stack {} — salt state '{}' is not present on this cluster "
                    + "(it predates the feature)", stack.getResourceCrn(), CONVERGENCE_SALT_STATE_SLS);
            return false;
        } else {
            return true;
        }
    }

    private Map<String, Object> buildConvergencePillar(Collection<String> excludedHosts) {
        return Map.of("freeipa", Map.of("replication_convergence", Map.<String, Object>of(
                "exclude_hosts", String.join(",", excludedHosts),
                "timeout_sec", convergenceTimeoutSec,
                "interval_sec", convergenceIntervalSec)));
    }

    /**
     * True only when the primary gateway's salt confirms that the state does not exist, i.e. the cluster predates the check. A probe that
     * keeps failing (salt-api timeout, unreachable gateway, unexpected answer) proves nothing, so it fails the check instead of skipping it:
     * skipping would let the operation continue, e.g. remove the former primary gateway, without replication having been verified.
     */
    private boolean verificationStateMissing(Stack stack, String sls, Function<CloudbreakOrchestratorException, String> executionError)
            throws CloudbreakOrchestratorFailedException {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            return !retryService.testWith1SecDelayMax3Times(() -> slsExists(primaryGatewayConfig, sls));
        } catch (Retry.ActionFailedException e) {
            CloudbreakOrchestratorFailedException probeFailure = new CloudbreakOrchestratorFailedException(String.format(
                    "Could not determine whether salt state '%s' is present on %s: %s", sls, primaryGatewayConfig.getHostname(), e.getMessage()),
                    e);
            LOGGER.error("Replication verification cannot run for stack {}", stack.getResourceCrn(), probeFailure);
            throw new CloudbreakOrchestratorFailedException(executionError.apply(probeFailure), probeFailure);
        }
    }

    private boolean slsExists(GatewayConfig primaryGatewayConfig, String sls) {
        try {
            return hostOrchestrator.doesPhaseSlsExistWithTimeouts(primaryGatewayConfig, sls, SLS_EXISTS_CONNECT_TIMEOUT_MS,
                    SLS_EXISTS_READ_TIMEOUT_MS);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Could not determine whether salt state '{}' is present on {}, the check will be retried: {}", sls,
                    primaryGatewayConfig.getHostname(), e.getMessage());
            throw Retry.ActionFailedException.ofCause(e);
        }
    }

    private OrchestratorStateRetryParams buildRetryParams(long scriptTimeoutSec) {
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setSleepTime(POLL_INTERVAL_SEC * MILLISEC_MULTIPLIER);
        retryParams.setMaxRetry((int) (scriptTimeoutSec / POLL_INTERVAL_SEC) + POLL_MARGIN);
        retryParams.setMaxRetryOnError(MAX_RETRY_ON_ERROR);
        return retryParams;
    }

    /**
     * The check scripts exit non-zero only on their own timeout and then print a timeout marker ("TIMEOUT after ..." for the cleanup
     * check, "... not healthy within ..." for the convergence check), which salt surfaces in the failed state's output. Any other failure
     * (e.g. salt returned null jid, unresponsive minions, a state that does not render) means the check never ran.
     */
    private boolean scriptTimedOut(CloudbreakOrchestratorException e, String timeoutMarker) {
        // SaltOrchestrator.runOrchestratorState re-wraps the runner's exception without its nodesWithErrors, so walk the cause chain.
        return ExceptionUtils.getThrowableList(e).stream()
                .filter(CloudbreakOrchestratorException.class::isInstance)
                .map(t -> ((CloudbreakOrchestratorException) t).getNodesWithErrors())
                .filter(Objects::nonNull)
                .map(Multimap::values)
                .flatMap(Collection::stream)
                .anyMatch(error -> error != null && error.contains(timeoutMarker));
    }

    private String buildCleanupExecutionError(Collection<String> removedHosts, CloudbreakOrchestratorException e) {
        return messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CLEANUP_EXECUTION_FAILED.getMessage(),
                String.join(", ", removedHosts), e.getMessage());
    }

    private String buildCleanupUserFacingError(Collection<String> removedHosts) {
        return messagesService.getMessageWithArgs(ResourceEvent.FREEIPA_REPLICATION_CLEANUP_FAILED.getMessage(),
                String.join(", ", removedHosts), String.valueOf(timeoutSec), CLEANUP_LOG);
    }

    private void performOptionalPreCheck(Stack stack, Collection<String> removedHosts) {
        try {
            FreeIpaClient client = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            Set<IpaServer> servers = client.findAllServers();
            boolean staleServersFound = servers.stream()
                    .map(IpaServer::getFqdn)
                    .anyMatch(removedHosts::contains);
            if (staleServersFound) {
                LOGGER.warn("Pre-check: some removed hosts {} still appear in 389-ds server list for stack {} (crn: {}). "
                        + "Salt state will enforce cleanup.", removedHosts, stack.getId(), stack.getResourceCrn());
            } else {
                LOGGER.debug("Pre-check: no removed hosts remain in 389-ds server list for stack {}", stack.getResourceCrn());
            }
        } catch (Exception e) {
            LOGGER.warn("Pre-check for replication cleanup failed (non-fatal) for stack {}: {}", stack.getResourceCrn(), e.getMessage());
        }
    }
}
