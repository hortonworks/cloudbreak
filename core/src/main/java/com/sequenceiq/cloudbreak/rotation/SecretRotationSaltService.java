package com.sequenceiq.cloudbreak.rotation;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;

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

    public void updateSaltPillar(StackDto stackDto, Map<String, SaltPillarProperties> servicePillar)
            throws CloudbreakOrchestratorFailedException {
        LOGGER.info("Salt pillar update, keys: {}", servicePillar.keySet());
        OrchestratorStateParams stateParams = saltStateParamsService.createStateParams(stackDto, null, true, MAX_RETRY, MAX_RETRY_ON_ERROR);
        ExitCriteriaModel exitCriteriaModel = exitCriteriaProvider.get(stackDto);
        hostOrchestrator.saveCustomPillars(new SaltConfig(servicePillar), exitCriteriaModel, stateParams);
    }

    public void validateSaltPrimaryGateway(StackDto stackDto) {
        OrchestratorStateParams stateParams = saltStateParamsService.createStateParams(stackDto, null, true, MAX_RETRY, MAX_RETRY_ON_ERROR);
        validateSalt(stateParams.getTargetHostNames(), stateParams.getPrimaryGatewayConfig());
    }

    public void validateSalt(StackDto stackDto) {
        GatewayConfig primaryGatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
        try {
            Map<String, Boolean> result = hostOrchestrator.ping(primaryGatewayConfig);
            checkPingResponse(result);
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException(e.getMessage(), e);
        }
    }

    public void validateSalt(Set<String> targets, GatewayConfig gatewayConfig) {
        try {
            Map<String, Boolean> result = hostOrchestrator.ping(targets, gatewayConfig);
            checkPingResponse(result);
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException(e);
        }
    }

    private void checkPingResponse(Map<String, Boolean> result) throws CloudbreakOrchestratorFailedException {
        Set<String> failedNodes = result.entrySet().stream()
                .filter(entry -> !Boolean.TRUE.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (!failedNodes.isEmpty()) {
            String message = String.format("Salt ping failed: %s", String.join(", ", failedNodes));
            LOGGER.warn(message);
            throw new CloudbreakOrchestratorFailedException(message);
        }
    }

    public void executeSaltState(GatewayConfig gatewayConfig, Set<String> targets, List<String> states, ExitCriteriaModel exitCriteriaModel,
            Optional<Integer> maxRetry, Optional<Integer> maxRetryOnError) throws CloudbreakOrchestratorFailedException {
        hostOrchestrator.executeSaltState(gatewayConfig, targets, states, exitCriteriaModel,
                maxRetry.or(() -> Optional.of(MAX_RETRY)), maxRetryOnError.or(() -> Optional.of(MAX_RETRY_ON_ERROR)));
    }

    public void executeSaltState(StackDto stackDto, Set<String> targets, List<String> states) throws CloudbreakOrchestratorFailedException {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
        ExitCriteriaModel exitCriteriaModel = exitCriteriaProvider.get(stackDto);
        executeSaltState(gatewayConfig, targets, states, exitCriteriaModel, Optional.of(MAX_RETRY), Optional.of(MAX_RETRY_ON_ERROR));
    }

    public void executeSaltStateOnPrimaryGateway(StackDto stackDto, List<String> states) throws CloudbreakOrchestratorFailedException {
        GatewayConfig gatewayConfig = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
        ExitCriteriaModel exitCriteriaModel = exitCriteriaProvider.get(stackDto);
        executeSaltState(gatewayConfig, Set.of(gatewayConfig.getHostname()), states, exitCriteriaModel, Optional.of(MAX_RETRY), Optional.of(MAX_RETRY_ON_ERROR));
    }

    public void executeSaltRun(OrchestratorStateParams stateParams) throws CloudbreakOrchestratorFailedException {
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    public Map<String, String> executeCommand(List<GatewayConfig> gatewayConfigs, Set<String> targets, String command)
            throws CloudbreakOrchestratorFailedException {
        return hostOrchestrator.runCommandOnHosts(gatewayConfigs, targets, command);
    }
}
