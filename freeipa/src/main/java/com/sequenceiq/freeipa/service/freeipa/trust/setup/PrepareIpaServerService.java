package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.rotation.SaltStateParamsService;
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
    private StackService stackService;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    public void prepareIpaServer(Long stackId) throws CloudbreakOrchestratorFailedException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        OrchestratorStateParams stateParams = createOrchestratorStateParams(stack);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createOrchestratorStateParams(Stack stack) {
        OrchestratorStateParams stateParameters = saltStateParamsService.createStateParams(stack, "trustsetup.adtrust_install", false,
                maxRetryCount, maxRetryCountOnError);
        LOGGER.debug("Created OrchestratorStateParams for running adtrust install: {}", stateParameters);
        return stateParameters;
    }
}
