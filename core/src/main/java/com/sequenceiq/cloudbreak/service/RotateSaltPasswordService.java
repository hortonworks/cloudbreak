package com.sequenceiq.cloudbreak.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.core.bootstrap.service.SaltBootstrapVersionChecker;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordType;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Service
public class RotateSaltPasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordService.class);

    private static final String SALTUSER_DELETE_COMMAND = "userdel saltuser";

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private ClusterBootstrapper clusterBootstrapper;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private ReactorFlowManager flowManager;

    @Inject
    private SaltPasswordStatusService saltPasswordStatusService;

    private Supplier<String> passwordGenerator = PasswordUtil::generatePassword;

    public void validateRotateSaltPassword(StackDto stack) {
        if (stack.getStatus().isStopped()) {
            throw new BadRequestException("Rotating SaltStack user password is not supported for stopped clusters");
        }
        if (!entitlementService.isSaltUserPasswordRotationEnabled(stack.getAccountId())) {
            throw new BadRequestException("Rotating SaltStack user password is not supported in your account");
        }
        if (stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().isEmpty()) {
            throw new IllegalStateException("Rotating SaltStack user password is not supported when there are no available gateway instances");
        }
        if (!isChangeSaltuserPasswordSupported(stack) && stack.getNotTerminatedInstanceMetaData().stream().anyMatch(im -> !im.isRunning())) {
            // fallback implementation re-bootstraps all nodes, so they have to be running
            throw new IllegalStateException("Rotating SaltStack user password is only supported when all instances are running");
        }
    }

    private boolean isChangeSaltuserPasswordSupported(StackDto stack) {
        return stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().stream()
                .map(InstanceMetadataView::getImage)
                .allMatch(image -> saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(image));
    }

    public FlowIdentifier triggerRotateSaltPassword(StackDto stack, RotateSaltPasswordReason reason) {
        validateRotateSaltPassword(stack);
        RotateSaltPasswordType rotateSaltPasswordType = getRotateSaltPasswordType(stack);
        LOGGER.info("Triggering rotate salt password for stack {} with type {}", stack.getId(), rotateSaltPasswordType);
        return flowManager.triggerRotateSaltPassword(stack.getId(), reason, rotateSaltPasswordType);
    }

    private RotateSaltPasswordType getRotateSaltPasswordType(StackDto stack) {
        return isChangeSaltuserPasswordSupported(stack) ? RotateSaltPasswordType.SALT_BOOTSTRAP_ENDPOINT : RotateSaltPasswordType.FALLBACK;
    }

    public void rotateSaltPassword(StackDto stack) throws CloudbreakOrchestratorException {
        validateRotateSaltPassword(stack);
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

    public void rotateSaltPasswordFallback(StackDto stack) throws CloudbreakOrchestratorFailedException {
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
