package com.sequenceiq.freeipa.service.rotation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.orchestrator.FreeIpaSaltPingService;
import com.sequenceiq.freeipa.service.orchestrator.SaltPingFailedException;

@Service
public class SecretRotationSaltService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretRotationSaltService.class);

    private static final int MAX_RETRY_ON_ERROR = 3;

    private static final int MAX_RETRY = 100;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private FreeIpaSaltPingService freeIpaSaltPingService;

    public void updateSaltPillar(Stack stack, Map<String, SaltPillarProperties> servicePillar)
            throws CloudbreakOrchestratorFailedException {
        LOGGER.info("Salt pillar update, keys: {}", servicePillar.keySet());
        OrchestratorStateParams stateParams = saltStateParamsService.createStateParams(stack, null, true, MAX_RETRY, MAX_RETRY_ON_ERROR);
        ExitCriteriaModel exitCriteriaModel = exitCriteriaProvider.get(stack);
        hostOrchestrator.saveCustomPillars(new SaltConfig(servicePillar), exitCriteriaModel, stateParams);
    }

    public void validateSalt(Stack stack) {
        OrchestratorStateParams stateParams = saltStateParamsService.createStateParams(stack, null, true, MAX_RETRY, MAX_RETRY_ON_ERROR);
        validateSalt(stateParams.getTargetHostNames(), stateParams.getPrimaryGatewayConfig());
    }

    public void validateSalt(Set<String> targets, GatewayConfig gatewayConfig) {
        try {
            freeIpaSaltPingService.saltPing(targets, gatewayConfig);
        } catch (SaltPingFailedException | CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException(e);
        }
    }

    public void executeSaltState(GatewayConfig gatewayConfig, Set<String> targets, List<String> states, ExitCriteriaModel exitCriteriaModel,
            Optional<Integer> maxRetry, Optional<Integer> maxRetryOnError) throws CloudbreakOrchestratorFailedException {
        hostOrchestrator.executeSaltState(gatewayConfig, targets, states, exitCriteriaModel,
                maxRetry.or(() -> Optional.of(MAX_RETRY)), maxRetryOnError.or(() -> Optional.of(MAX_RETRY_ON_ERROR)));
    }

    public void executeSaltState(Stack stack, Set<String> targets, List<String> states) throws CloudbreakOrchestratorFailedException {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stack);
        ExitCriteriaModel exitCriteriaModel = exitCriteriaProvider.get(stack);
        executeSaltState(gatewayConfig, targets, states, exitCriteriaModel, Optional.of(MAX_RETRY), Optional.of(MAX_RETRY_ON_ERROR));
    }

    public void executeSaltRun(OrchestratorStateParams stateParams) throws CloudbreakOrchestratorFailedException {
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public Map<String, String> executeCommand(List<GatewayConfig> gatewayConfigs, Set<String> targets, String command)
            throws CloudbreakOrchestratorFailedException {
        return hostOrchestrator.runCommandOnHosts(gatewayConfigs, targets, command);
    }
}
