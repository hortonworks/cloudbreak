package com.sequenceiq.freeipa.service.upgrade.ccm;

import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorRunParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.service.orchestrator.OrchestratorParamsProvider;

@Service
public class UpgradeCcmOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmOrchestratorService.class);

    private static final String UPGRADE_CCM_STATE = "upgradeccm";

    private static final String NGINX_STATE = "nginx";

    private static final String DISABLE_MINA_STATE = "upgradeccm/disable-ccmv1";

    private static final String FINALIZE = "upgradeccm/finalize";

    private static final String CONNECTIVITY_CHECK_COMMAND = "ccmv2-connectivity-check.sh %s";

    @Inject
    private OrchestratorParamsProvider orchestratorParamsProvider;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void applyUpgradeState(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = orchestratorParamsProvider.createStateParams(stackId, UPGRADE_CCM_STATE);
        LOGGER.debug("Calling applyUpgradeState with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void reconfigureNginx(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = orchestratorParamsProvider.createStateParams(stackId, NGINX_STATE);
        LOGGER.debug("Calling reconfigureNginx with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void finalizeConfiguration(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = orchestratorParamsProvider.createStateParams(stackId, FINALIZE);
        LOGGER.debug("Calling finalize with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void disableMina(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = orchestratorParamsProvider.createStateParams(stackId, DISABLE_MINA_STATE);
        LOGGER.debug("Calling disableMina with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public Map<String, String> checkCcmV2Connectivity(Long stackId, String cidr) {
        OrchestratorRunParams runParams = orchestratorParamsProvider.createRunParams(stackId,
                String.format(CONNECTIVITY_CHECK_COMMAND, cidr), "Failed to determine CCMv2 Connectivity.");
        LOGGER.debug("Calling checkCcmV2Connectivity with run params '{}'", runParams);
        return hostOrchestrator.runShellCommandOnNodes(runParams);
    }
}
