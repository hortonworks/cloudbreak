package com.sequenceiq.cloudbreak.service.proxy;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.service.orchestrator.OrchestratorStateParamsProvider;

@Service
public class ModifyProxyConfigService {

    static final String MODIFY_PROXY_STATE = "modifyproxy";

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProxyConfigService.class);

    @Inject
    private OrchestratorStateParamsProvider orchestratorStateParamsProvider;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ClusterBuilderService clusterBuilderService;

    public void applyModifyProxyState(Long stackId) throws CloudbreakOrchestratorException {
        OrchestratorStateParams stateParams = orchestratorStateParamsProvider.createStateParams(stackId, MODIFY_PROXY_STATE);
        LOGGER.debug("Calling modifyproxy with state params '{}'", stateParams);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public void updateClusterManager(Long stackId) {
        LOGGER.debug("Updating CM with modified proxy config settings");
        clusterBuilderService.modifyProxyConfig(stackId);
    }
}
