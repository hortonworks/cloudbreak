package com.sequenceiq.cloudbreak.service.salt;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Service
public class RotateSaltPasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordService.class);

    private static final String SALTUSER_DELETE_COMMAND = "userdel saltuser";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private SaltPasswordStatusService saltPasswordStatusService;

    @Inject
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    private Supplier<String> passwordGenerator = PasswordUtil::generatePassword;

    public void rotateSaltPassword(StackDto stack) throws CloudbreakOrchestratorException {
        rotateSaltPasswordValidator.validateRotateSaltPassword(stack);
        if (rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)) {
            rotateSaltPasswordChangePassword(stack);
        } else {
            rotateSaltPasswordFallback(stack);
        }
    }

    private void rotateSaltPasswordChangePassword(StackDto stack) throws CloudbreakOrchestratorException {
        SecurityConfig securityConfig = stack.getSecurityConfig();
        String oldPassword = securityConfig.getSaltSecurityConfig().getSaltPassword();
        String newPassword = passwordGenerator.get();
        List<GatewayConfig> allGatewayConfig = gatewayConfigService.getAllGatewayConfigs(stack);
        hostOrchestrator.changePassword(allGatewayConfig, newPassword, oldPassword);
        securityConfig.getSaltSecurityConfig().setSaltPassword(newPassword);
        validateAndSavePassword(stack, newPassword);
    }

    private void validateAndSavePassword(StackDto stack, String newPassword) throws CloudbreakOrchestratorFailedException {
        SaltPasswordStatus saltPasswordStatus = saltPasswordStatusService.getSaltPasswordStatus(stack);
        if (saltPasswordStatus != SaltPasswordStatus.OK) {
            String message = String.format("Salt password status check failed with status %s, please try the operation again", saltPasswordStatus);
            throw new CloudbreakOrchestratorFailedException(message);
        }
        securityConfigService.changeSaltPassword(stack.getSecurityConfig(), newPassword);
    }

    private void rotateSaltPasswordFallback(StackDto stack) throws CloudbreakOrchestratorFailedException {
        List<GatewayConfig> allGatewayConfig = gatewayConfigService.getAllGatewayConfigs(stack);
        tryRemoveSaltuserFromGateways(stack, allGatewayConfig);

        String newPassword = passwordGenerator.get();
        SecurityConfig securityConfig = stack.getSecurityConfig();
        securityConfig.getSaltSecurityConfig().setSaltPassword(newPassword);
        try {
            clusterBootstrapper.reBootstrapGateways(stack);
            validateAndSavePassword(stack, newPassword);
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

    public void sendSuccessUsageReport(String resourceCrn, RotateSaltPasswordReason reason) {
        sendUsageReport(resourceCrn, reason, UsageProto.CDPSaltPasswordRotationEventResult.Value.SUCCESS, "");
    }

    public void sendFailureUsageReport(String resourceCrn, RotateSaltPasswordReason reason, String message) {
        sendUsageReport(resourceCrn, reason, UsageProto.CDPSaltPasswordRotationEventResult.Value.FAILURE, message);
    }

    private void sendUsageReport(String resourceCrn, RotateSaltPasswordReason reason, UsageProto.CDPSaltPasswordRotationEventResult.Value result,
            String message) {
        try {
            LOGGER.info("Reporting rotate salt password event with resource crn {}, reason {}, result {} and message {}",
                    resourceCrn, reason, result, message);
            UsageProto.CDPSaltPasswordRotationEvent event = UsageProto.CDPSaltPasswordRotationEvent.newBuilder()
                    .setResourceCrn(Objects.requireNonNull(resourceCrn))
                    .setReason(UsageProto.CDPSaltPasswordRotationEventReason.Value.valueOf(Objects.requireNonNull(reason).name()))
                    .setEventResult(Objects.requireNonNull(result))
                    .setMessage(Objects.requireNonNullElse(message, ""))
                    .build();
            usageReporter.cdpSaltPasswordRotationEvent(event);
        } catch (Exception e) {
            LOGGER.error("Failed to report rotate salt password event with resource crn {}, reason {}, result {} and message {}",
                    resourceCrn, reason, result, message, e);
        }
    }
}
