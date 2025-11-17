package com.sequenceiq.freeipa.orchestrator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerConfig;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationValidationService;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationStepProgressService;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.dto.RotateSaltPasswordReason;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.rotation.FreeIpaSecretRotationService;
import com.sequenceiq.freeipa.util.SaltBootstrapVersionChecker;

@Service
public class RotateSaltPasswordService {

    protected static final String SALTUSER_DELETE_COMMAND = "userdel saltuser";

    protected static final String SALTUSER = "saltuser";

    protected static final String UNAUTHORIZED_RESPONSE = "Status: 401 Unauthorized Response";

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordService.class);

    @Inject
    private SaltStatusCheckerConfig saltStatusCheckerConfig;

    @Inject
    private Clock clock;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Inject
    private BootstrapService bootstrapService;

    @Inject
    private FreeIpaSecretRotationService freeIpaSecretRotationService;

    @Inject
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Inject
    private SecretRotationValidationService secretRotationValidationService;

    @Inject
    private SaltUpdateService saltUpdateService;

    @Inject
    private SecretRotationStepProgressService secretRotationStepProgressService;

    public FlowIdentifier triggerRotateSaltPassword(String environmentCrn, String accountId, RotateSaltPasswordReason reason) {
        if (secretRotationValidationService.failedRotationAlreadyHappened(environmentCrn, FreeIpaSecretType.SALT_PASSWORD)) {
            secretRotationStepProgressService.deleteCurrentRotation(RotationMetadata.builder()
                    .secretType(FreeIpaSecretType.SALT_PASSWORD)
                    .resourceCrn(environmentCrn)
                    .build());
            LOGGER.info("Since there is already a failed salt password rotation for freeipa of environment {}, " +
                    "we are doing salt update to initiate a salt rebootstrap and resolve the issue with saltuser.", environmentCrn);
            return saltUpdateService.updateSaltStates(environmentCrn, accountId);
        } else {
            LOGGER.info("Triggering rotate salt password for freeipa of environment {}", environmentCrn);
            FreeIpaSecretRotationRequest request = new FreeIpaSecretRotationRequest();
            request.setSecrets(List.of(FreeIpaSecretType.SALT_PASSWORD.value()));
            return freeIpaSecretRotationService.rotateSecretsByCrn(accountId, environmentCrn, request);
        }
    }

    public void rotateSaltPassword(Stack stack) {
        if (saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)) {
            rotateSaltPasswordChangePassword(stack);
        } else {
            rotateSaltPasswordFallback(stack);
        }
    }

    public void validatePasswordAfterRotation(Stack stack) {
        Optional<RotateSaltPasswordReason> rotateSaltPasswordReason = checkIfSaltPasswordRotationNeeded(stack);
        if (rotateSaltPasswordReason.isPresent()) {
            String message = String.format("Salt password status check failed with status %s, please try the operation again", rotateSaltPasswordReason.get());
            throw new SecretRotationException(message);
        }
    }

    private void rotateSaltPasswordChangePassword(Stack stack) {
        try {
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
            RotationSecret password =
                    uncachedSecretServiceForRotation.getRotation(stack.getSecurityConfig().getSaltSecurityConfig().getSaltPasswordVaultSecret());
            hostOrchestrator.changePassword(gatewayConfigs, password.getSecret(), password.getBackupSecret());
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Failed to rotate salt password", e);
            throw new SecretRotationException(e.getMessage(), e);
        }
    }

    private void rotateSaltPasswordFallback(Stack stack) {
        List<GatewayConfig> allGatewayConfig = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
        tryRemoveSaltuserFromGateways(stack, allGatewayConfig);
        try {
            bootstrapService.reBootstrap(stack);
        } catch (CloudbreakOrchestratorException e) {
            Set<String> gatewayConfigAddresses = allGatewayConfig.stream()
                    .map(GatewayConfig::getPrivateAddress)
                    .collect(Collectors.toSet());
            LOGGER.warn("Failed to re-bootstrap gateway nodes after saltuser password delete", e);
            String message = String.format("Failed to re-bootstrap gateway nodes after saltuser password delete. " +
                            "Please check the salt-bootstrap service status on node(s) %s. " +
                            "If the saltuser password was changed manually, " +
                            "please remove the user manually with the command '%s' on node(s) %s and retry the operation.",
                    gatewayConfigAddresses, SALTUSER_DELETE_COMMAND, gatewayConfigAddresses);
            throw new SecretRotationException(message, e);
        }
    }

    private void tryRemoveSaltuserFromGateways(Stack stack, List<GatewayConfig> allGatewayConfig) {
        try {
            Set<String> targets = stack.getAllFunctioningNodes().stream().map(Node::getHostname).collect(Collectors.toSet());
            Map<String, String> response = hostOrchestrator.runCommandOnHosts(allGatewayConfig, targets, SALTUSER_DELETE_COMMAND);
            LOGGER.debug("Saltuser delete command response: {}", response);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.warn("Failed to run saltuser delete command, assuming it is already deleted", e);
        }
    }

    public Optional<RotateSaltPasswordReason> checkIfSaltPasswordRotationNeeded(Stack stack) {
        Optional<RotateSaltPasswordReason> result;
        try {
            List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
            LocalDate passwordExpiryDate = hostOrchestrator.getPasswordExpiryDate(allGatewayConfigs, SALTUSER);
            if (isPasswordExpiresSoon(passwordExpiryDate)) {
                LOGGER.info("Stack {} user {} password expires at {}, password rotation is needed", stack.getId(), SALTUSER, passwordExpiryDate);
                result = Optional.of(RotateSaltPasswordReason.EXPIRED);
            } else {
                LOGGER.info("Stack {} user {} password expires at {}, nothing to do", stack.getId(), SALTUSER, passwordExpiryDate);
                result = Optional.empty();
            }
        } catch (CloudbreakOrchestratorException e) {
            if (isUnauthorizedException(e)) {
                LOGGER.info("Received unauthorized response from salt on stack {}", stack.getId());
                result = Optional.of(RotateSaltPasswordReason.UNAUTHORIZED);
            } else {
                LOGGER.warn("Received error response from salt on stack {}", stack.getId(), e);
                throw new CloudbreakRuntimeException(e);
            }
        }
        return result;
    }

    private boolean isPasswordExpiresSoon(LocalDate passwordExpiryDate) {
        long daysUntilPasswordExpires = ChronoUnit.DAYS.between(clock.getCurrentLocalDateTime(), passwordExpiryDate.atStartOfDay());
        return daysUntilPasswordExpires <= saltStatusCheckerConfig.getPasswordExpiryThresholdInDays();
    }

    private static boolean isUnauthorizedException(CloudbreakOrchestratorException e) {
        return Optional.ofNullable(e.getCause())
                .map(Throwable::getCause)
                .filter(ex -> ex.getMessage().startsWith(UNAUTHORIZED_RESPONSE))
                .isPresent();
    }
}
