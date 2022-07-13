package com.sequenceiq.freeipa.orchestrator;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordReason;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordRequest;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.SaltBootstrapVersionChecker;

@Service
public class RotateSaltPasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordService.class);

    @Inject
    private StackService stackService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Inject
    private FreeIpaFlowManager flowManager;

    @Inject
    private UsageReporter usageReporter;

    public FlowIdentifier triggerRotateSaltPassword(String environmentCrn, String accountId, RotateSaltPasswordReason reason) {
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        validateRotateSaltPassword(stack);
        String selector = RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_EVENT.event();
        return flowManager.notify(selector, new RotateSaltPasswordRequest(selector, stack.getId(), reason));
    }

    public void rotateSaltPassword(Stack stack) {
        validateRotateSaltPassword(stack);
        try {
            String oldPassword = stack.getSecurityConfig().getSaltSecurityConfig().getSaltPassword();
            String newPassword = PasswordUtil.generatePassword();
            List<GatewayConfig> gatewayConfigs = gatewayConfigService.getNotDeletedGatewayConfigs(stack);
            hostOrchestrator.changePassword(gatewayConfigs, newPassword, oldPassword);
            securityConfigService.changeSaltPassword(stack, newPassword);
        } catch (CloudbreakOrchestratorException e) {
            LOGGER.error("Failed to rotate salt password", e);
            throw new CloudbreakServiceException(e.getMessage(), e);
        }
    }

    private void validateRotateSaltPassword(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        if (!entitlementService.isSaltUserPasswordRotationEnabled(stack.getAccountId())) {
            throw new BadRequestException("Rotating SaltStack user password is not supported in your account");
        }
        if (!saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)) {
            throw new BadRequestException("Rotating SaltStack user password is not supported with your image version, " +
                    "please upgrade to an image with salt-bootstrap version >= 0.13.6 (you can find this information in the image catalog)");
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
