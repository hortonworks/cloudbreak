package com.sequenceiq.cloudbreak.service;

import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.SaltBootstrapVersionChecker;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Service
public class RotateSaltPasswordService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateSaltPasswordService.class);

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

    public void validateRotateSaltPassword(StackDto stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException("Rotating SaltStack user password is only available for stacks in available status");
        }
        if (!isChangeSaltuserPasswordSupported(stack)) {
            throw new BadRequestException(String.format("Rotating SaltStack user password is not supported with your image version, " +
                            "please upgrade to an image with salt-bootstrap version >= %s (you can find this information in the image catalog)",
                    SaltBootstrapVersionChecker.CHANGE_SALTUSER_PASSWORD_SUPPORT_MIN_VERSION));
        }
    }

    private boolean isChangeSaltuserPasswordSupported(StackDto stack) {
        return stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata().stream()
                .map(InstanceMetadataView::getImage)
                .allMatch(image -> saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(image));
    }

    public void rotateSaltPassword(StackDto stack) throws CloudbreakOrchestratorException {
        validateRotateSaltPassword(stack);
        SecurityConfig securityConfig = securityConfigService.getOneByStackId(stack.getId());
        String oldPassword = securityConfig.getSaltSecurityConfig().getSaltPassword();
        String newPassword = PasswordUtil.generatePassword();
        List<GatewayConfig> allGatewayConfig = gatewayConfigService.getAllGatewayConfigs(stack);
        hostOrchestrator.changePassword(allGatewayConfig, newPassword, oldPassword);
        securityConfigService.changeSaltPassword(securityConfig, newPassword);
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
