package com.sequenceiq.cloudbreak.service.salt;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;

@Service
public class RotateSaltPasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordService.class);

    private static final String SALTUSER_DELETE_COMMAND = "userdel saltuser";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private SaltPasswordStatusService saltPasswordStatusService;

    @Inject
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    public void rotateSaltPassword(StackDto stack) throws CloudbreakOrchestratorException {
        if (rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)) {
            rotateSaltPasswordChangePassword(stack);
        } else {
            rotateSaltPasswordFallback(stack);
        }
    }

    public void validatePasswordAfterRotation(StackDto stack) {
        SaltPasswordStatus saltPasswordStatus = saltPasswordStatusService.getSaltPasswordStatus(stack);
        if (saltPasswordStatus != SaltPasswordStatus.OK) {
            String message = String.format("Salt password status check failed with status %s, please try the operation again", saltPasswordStatus);
            throw new SecretRotationException(message);
        }
    }

    private void rotateSaltPasswordChangePassword(StackDto stack) throws CloudbreakOrchestratorException {
        List<GatewayConfig> allGatewayConfig = gatewayConfigService.getAllGatewayConfigs(stack);
        RotationSecret password = uncachedSecretServiceForRotation.getRotation(stack.getSecurityConfig().getSaltSecurityConfig().getSaltPasswordSecret());
        hostOrchestrator.changePassword(allGatewayConfig, password.getSecret(), password.getBackupSecret());
    }

    private void rotateSaltPasswordFallback(StackDto stack) throws CloudbreakOrchestratorFailedException {
        List<GatewayConfig> allGatewayConfig = gatewayConfigService.getAllGatewayConfigs(stack);
        tryRemoveSaltuserFromGateways(stack, allGatewayConfig);
        try {
            clusterBootstrapper.reBootstrapGateways(stack);
        } catch (Exception e) {
            Set<String> gatewayConfigAddresses = allGatewayConfig.stream()
                    .map(GatewayConfig::getPrivateAddress)
                    .collect(Collectors.toSet());
            LOGGER.warn("Failed to re-bootstrap gateway nodes after saltuser password delete", e);
            String message = String.format("Failed to re-bootstrap gateway nodes after saltuser password delete. " +
                            "Please check the salt-bootstrap service status on node(s) %s. " +
                            "If the saltuser password was changed manually, " +
                            "please remove the user manually with the command '%s' on node(s) %s and retry the operation.",
                    gatewayConfigAddresses, SALTUSER_DELETE_COMMAND, gatewayConfigAddresses);
            throw new CloudbreakOrchestratorFailedException(message, e);
        }
    }

    private void tryRemoveSaltuserFromGateways(StackDto stack, List<GatewayConfig> allGatewayConfig) {
        try {
            Set<String> targetFqdns = stack.getAllPrimaryGatewayInstanceNodes().stream().map(Node::getHostname).collect(Collectors.toSet());
            Map<String, String> response = hostOrchestrator.runCommandOnHosts(allGatewayConfig, targetFqdns, SALTUSER_DELETE_COMMAND);
            LOGGER.debug("Saltuser delete command response: {}", response);
        } catch (CloudbreakOrchestratorFailedException e) {
            LOGGER.warn("Failed to run saltuser delete command, assuming it is already deleted", e);
        }
    }
}
