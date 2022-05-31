package com.sequenceiq.freeipa.orchestrator;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.util.PasswordUtil;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
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

    public void rotateSaltPassword(String environmentCrn, String accountId) {
        if (!entitlementService.isSaltUserPasswordRotationEnabled(accountId)) {
            throw new BadRequestException("Rotating salt password is not supported in your account");
        }

        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrn, accountId);
        MDCBuilder.buildMdcContext(stack);

        if (!saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)) {
            throw new BadRequestException("Rotating salt password is not supported with your image version, " +
                    "please upgrade to an image with salt-bootstrap version >= 0.13.6 (you can find this information in the image catalog)");
        }

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
}
