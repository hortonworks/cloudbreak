package com.sequenceiq.freeipa.service.freeipa.cleanup;

import static com.sequenceiq.cloudbreak.util.TimeUtil.MILLISEC_MULTIPLIER;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
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

    private static final String SALT_STATE = "freeipa/verify-replication-cleanup";

    private static final String SALT_STATE_SLS = "freeipa.verify-replication-cleanup";

    private static final int SLS_EXISTS_CONNECT_TIMEOUT_MS = 5_000;

    private static final int SLS_EXISTS_READ_TIMEOUT_MS = 15_000;

    private static final int POLL_INTERVAL_SEC = 10;

    private static final int POLL_MARGIN = 12;

    private static final int MAX_RETRY_ON_ERROR = 2;

    @Value("${freeipa.replication.cleanup.polling.timeout-sec:600}")
    private long timeoutSec;

    @Value("${freeipa.replication.cleanup.polling.interval-sec:10}")
    private long intervalSec;

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

    public void verifyOnSurvivingMasters(Long stackId, Collection<String> removedHosts) throws CloudbreakOrchestratorException {
        if (removedHosts == null || removedHosts.isEmpty()) {
            LOGGER.debug("No removed hosts provided, skipping replication cleanup verification for stack {}", stackId);
        } else {
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            int survivingNodeCount = stack.getNotDeletedInstanceMetaDataSet().size();
            if (!verificationStateAvailable(stack)) {
                LOGGER.info("Skipping replication cleanup verification for stack {} — salt state '{}' is not present on this cluster "
                        + "(it predates the feature)", stackId, SALT_STATE_SLS);
            } else {
                performOptionalPreCheck(stack, removedHosts);
                LOGGER.info("Running replication cleanup verification on {} surviving master(s) for stack {}, removed hosts: {}",
                        survivingNodeCount, stackId, removedHosts);
                OrchestratorStateParams stateParams = orchestratorParamsProvider.createStateParams(stackId, SALT_STATE);
                stateParams.setStateParams(Map.of("freeipa", Map.of("replication_cleanup", Map.<String, Object>of(
                        "removed_hosts", String.join(",", removedHosts),
                        "timeout_sec", timeoutSec,
                        "interval_sec", intervalSec))));
                stateParams.setStateRetryParams(buildRetryParams());
                try {
                    hostOrchestrator.runOrchestratorState(stateParams);
                    LOGGER.info("Replication cleanup verification completed successfully for stack {}", stackId);
                } catch (CloudbreakOrchestratorException e) {
                    LOGGER.error("Replication cleanup verification failed for stack {}, removed hosts: {}", stackId, removedHosts, e);
                    throw new CloudbreakOrchestratorFailedException(buildUserFacingError(removedHosts), e);
                }
            }
        }
    }

    private boolean verificationStateAvailable(Stack stack) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        try {
            return hostOrchestrator.doesPhaseSlsExistWithTimeouts(primaryGatewayConfig, SALT_STATE_SLS,
                    SLS_EXISTS_CONNECT_TIMEOUT_MS, SLS_EXISTS_READ_TIMEOUT_MS);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Could not determine whether salt state '{}' is present for stack {}; skipping replication cleanup "
                    + "verification to avoid blocking downscale: {}", SALT_STATE_SLS, stack.getId(), e.getMessage());
            return false;
        }
    }

    private OrchestratorStateRetryParams buildRetryParams() {
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setSleepTime(POLL_INTERVAL_SEC * MILLISEC_MULTIPLIER);
        retryParams.setMaxRetry((int) (timeoutSec / POLL_INTERVAL_SEC) + POLL_MARGIN);
        retryParams.setMaxRetryOnError(MAX_RETRY_ON_ERROR);
        return retryParams;
    }

    private String buildUserFacingError(Collection<String> removedHosts) {
        return String.format(
                "FreeIPA replication cleanup did not converge within %d seconds for removed host(s): %s. "
                        + "A surviving FreeIPA master is still running a CleanAllRUV task or holds a lingering replication "
                        + "agreement for the removed host(s), which would block the next replica install. Action items: on a "
                        + "surviving FreeIPA master inspect 'cn=cleanallruv,cn=tasks,cn=config' for stuck tasks; if a task is "
                        + "wedged run 'ipa-replica-manage clean-dangling-ruv' or delete the stuck task entry, then retry the "
                        + "operation. See /var/log/freeipa_replication_cleanup.log on the master(s) for the detailed check output.",
                timeoutSec, String.join(", ", removedHosts));
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
                LOGGER.debug("Pre-check: no removed hosts remain in 389-ds server list for stack {}", stack.getId());
            }
        } catch (Exception e) {
            LOGGER.warn("Pre-check for replication cleanup failed (non-fatal) for stack {}: {}", stack.getId(), e.getMessage());
        }
    }
}
