package com.sequenceiq.cloudbreak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerConfig;

@ExtendWith(MockitoExtension.class)
class SaltPasswordStatusServiceTest {

    private static final String ACCOUNT_ID = "0";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private Clock clock;

    @Mock
    private SaltStatusCheckerConfig saltStatusCheckerConfig;

    @Mock
    private StackDto stack;

    @Mock
    private List<GatewayConfig> gatewayConfigs;

    @InjectMocks
    private SaltPasswordStatusService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(entitlementService.isSaltUserPasswordRotationEnabled(ACCOUNT_ID)).thenReturn(true);
    }

    @Test
    void expiredSaltPasswordRotationNeeded() throws Exception {
        when(clock.getCurrentLocalDateTime()).thenReturn(LocalDateTime.now());
        when(saltStatusCheckerConfig.getPasswordExpiryThresholdInDays()).thenReturn(14);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(hostOrchestrator.getPasswordExpiryDate(gatewayConfigs, SaltPasswordStatusService.SALTUSER)).thenReturn(LocalDate.now().minusMonths(2));

        SaltPasswordStatus result = underTest.getSaltPasswordStatus(stack);

        assertThat(result).isEqualTo(SaltPasswordStatus.EXPIRES);
    }

    @Test
    void noSaltPasswordRotationNeeded() throws Exception {
        when(clock.getCurrentLocalDateTime()).thenReturn(LocalDateTime.now());
        when(saltStatusCheckerConfig.getPasswordExpiryThresholdInDays()).thenReturn(14);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(hostOrchestrator.getPasswordExpiryDate(gatewayConfigs, SaltPasswordStatusService.SALTUSER)).thenReturn(LocalDate.now().plusMonths(2));

        SaltPasswordStatus result = underTest.getSaltPasswordStatus(stack);

        assertThat(result).isEqualTo(SaltPasswordStatus.OK);
    }

    @Test
    void unauthorizedSaltPasswordRotationNeeded() throws Exception {
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        RuntimeException causeCause = new RuntimeException(SaltPasswordStatusService.UNAUTHORIZED_RESPONSE);
        RuntimeException cause = new RuntimeException("Ooops", causeCause);
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("Failed", cause);
        when(hostOrchestrator.getPasswordExpiryDate(gatewayConfigs, SaltPasswordStatusService.SALTUSER)).thenThrow(exception);

        SaltPasswordStatus result = underTest.getSaltPasswordStatus(stack);

        assertThat(result).isEqualTo(SaltPasswordStatus.INVALID);
    }

    @Test
    void errorWhileSaltPasswordRotationNeeded() throws Exception {
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("Unexpected failure");
        when(hostOrchestrator.getPasswordExpiryDate(gatewayConfigs, SaltPasswordStatusService.SALTUSER)).thenThrow(exception);

        SaltPasswordStatus result = underTest.getSaltPasswordStatus(stack);

        assertThat(result).isEqualTo(SaltPasswordStatus.FAILED_TO_CHECK);
    }

}