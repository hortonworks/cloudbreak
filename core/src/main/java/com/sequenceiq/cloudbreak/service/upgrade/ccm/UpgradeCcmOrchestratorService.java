package com.sequenceiq.cloudbreak.service.upgrade.ccm;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorStateParamsProvider;

@Service
public class UpgradeCcmOrchestratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeCcmOrchestratorService.class);

    private static final String UPGRADECCM_STATE = "nginx/upgradeccm";

    private static final String FINALIZE = "nginx/finalize";

    private static final String DISABLE_MINA_STATE = "upgradeccm/disable-ccmv1";

    private static final String DISABLE_INVERTING_PROXY_AGENT_STATE = "upgradeccm/disable-ccmv2";

    @Inject
    private OrchestratorStateParamsProvider orchestratorStateParamsProvider;

    @Inject
    private HostOrchestrator hostOrchestrator;

    public void reconfigureNginx(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = orchestratorStateParamsProvider.createStateParams(stackId, UPGRADECCM_STATE);
        LOGGER.debug("Calling reconfigureNginx with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void finalizeCcmOperation(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = orchestratorStateParamsProvider.createStateParams(stackId, FINALIZE);
        LOGGER.debug("Calling finalize with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void disableMina(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = orchestratorStateParamsProvider.createStateParams(stackId, DISABLE_MINA_STATE);
        LOGGER.debug("Calling disableMina with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void disableInvertingProxyAgent(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = orchestratorStateParamsProvider.createStateParams(stackId, DISABLE_INVERTING_PROXY_AGENT_STATE);
        LOGGER.debug("Calling disableInvertingProxyAgent with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }
}
