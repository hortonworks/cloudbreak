package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class PrepareIpaServerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrepareIpaServerService.class);

    @Value("${freeipa.max.salt.trustsetup.maxretry}")
    private int maxRetryCount;

    @Value("${freeipa.max.salt.trustsetup.maxerrorretry}")
    private int maxRetryCountOnError;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackService stackService;

    public void prepareIpaServer(Long stackId) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        OrchestratorStateParams stateParams = createOrchestratorStateParams(primaryGatewayConfig, stackId);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createOrchestratorStateParams(GatewayConfig primaryGatewayConfig, Long stackId) {
        OrchestratorStateParams stateParameters = new OrchestratorStateParams();
        stateParameters.setPrimaryGatewayConfig(primaryGatewayConfig);
        stateParameters.setTargetHostNames(Set.of(primaryGatewayConfig.getHostname()));
        stateParameters.setExitCriteriaModel(new StackBasedExitCriteriaModel(stackId));
        OrchestratorStateRetryParams stateRetryParams = new OrchestratorStateRetryParams();
        stateRetryParams.setMaxRetry(maxRetryCount);
        stateRetryParams.setMaxRetryOnError(maxRetryCountOnError);
        stateParameters.setStateRetryParams(stateRetryParams);
        stateParameters.setState("trustsetup.adtrust_install");
        LOGGER.debug("Created OrchestratorStateParams for running adtrust install: {}", stateParameters);
        return stateParameters;
    }
}
