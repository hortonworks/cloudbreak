package com.sequenceiq.cloudbreak.service.salt;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;

@Service
public class SaltPasswordStatusService {

    private static final String SALTUSER = "saltuser";

    private static final String UNAUTHORIZED_RESPONSE = "Status: 401 Unauthorized Response";

    private static final Logger LOGGER = LoggerFactory.getLogger(SaltPasswordStatusService.class);

    @Inject
    private Clock clock;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private SaltStatusCheckerConfig saltStatusCheckerConfig;

    public SaltPasswordStatus getSaltPasswordStatus(StackDto stack) {
        SaltPasswordStatus result;
        try {
            List<GatewayConfig> allGatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
            LocalDate passwordExpiryDate = hostOrchestrator.getPasswordExpiryDate(allGatewayConfigs, SALTUSER);
            if (isPasswordExpiresSoon(passwordExpiryDate)) {
                LOGGER.info("Stack {} user {} password expires at {}, password rotation is needed", stack.getId(), SALTUSER, passwordExpiryDate);
                result = SaltPasswordStatus.EXPIRES;
            } else {
                LOGGER.info("Stack {} user {} password expires at {}, nothing to do", stack.getId(), SALTUSER, passwordExpiryDate);
                result = SaltPasswordStatus.OK;
            }
        } catch (CloudbreakOrchestratorException e) {
            if (isUnauthorizedException(e)) {
                LOGGER.info("Received unauthorized response from salt on stack {}", stack.getId());
                result = SaltPasswordStatus.INVALID;
            } else {
                LOGGER.warn("Received error response from salt on stack {}", stack.getId(), e);
                result = SaltPasswordStatus.FAILED_TO_CHECK;
            }
        } catch (NotFoundException e) {
            if (!"No reachable gateway found".equals(e.getMessage())) {
                LOGGER.warn("Unhandled NotFoundException", e);
            }
            result = SaltPasswordStatus.FAILED_TO_CHECK;
        } catch (Exception e) {
            LOGGER.warn("Unhandled exception type", e);
            result = SaltPasswordStatus.FAILED_TO_CHECK;
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
