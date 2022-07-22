package com.sequenceiq.cloudbreak.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.core.bootstrap.service.SaltBootstrapVersionChecker;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerConfig;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordServiceTest {

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:cloudera:datalake:33071a14-d605-4b2d-9a55-218c0dbc95e3";

    private static final String ACCOUNT_ID = "0";

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

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

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    private ArgumentCaptor<UsageProto.CDPSaltPasswordRotationEvent> eventArgumentCaptor;

    @InjectMocks
    private RotateSaltPasswordService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(entitlementService.isSaltUserPasswordRotationEnabled(ACCOUNT_ID)).thenReturn(true);
    }

    @Test
    public void testRotateSaltPasswordSuccess() throws Exception {
        GatewayConfig gw1 = new GatewayConfig("host1", "1.1.1.1", "1.1.1.1", 22, "i-1839", false);
        GatewayConfig gw2 = new GatewayConfig("host2", "1.1.1.2", "1.1.1.2", 22, "i-1839", false);
        List<GatewayConfig> gatewayConfigs = List.of(gw1, gw2);
        when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(gatewayConfigs);

        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPassword("old-password");
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        when(securityConfigService.getOneByStackId(stack.getId())).thenReturn(securityConfig);

        underTest.rotateSaltPassword(stack);

        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(hostOrchestrator).changePassword(eq(gatewayConfigs), stringArgumentCaptor.capture(), eq(saltSecurityConfig.getSaltPassword()));
        String newPassword = stringArgumentCaptor.getValue();
        verify(securityConfigService).changeSaltPassword(securityConfig, newPassword);
    }

    @Test
    public void testRotateSaltPasswordOnStackInAccountWithoutEntitlement() {
        when(entitlementService.isSaltUserPasswordRotationEnabled(ACCOUNT_ID)).thenReturn(false);

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Rotating SaltStack user password is not supported in your account");
    }

    @Test
    public void testRotateSaltPasswordOnStackWithOldSBVersion() {
        when(stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(List.of(new InstanceMetaData()));
        when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(any())).thenReturn(false);

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Rotating SaltStack user password is not supported with your image version, " +
                        "please upgrade to an image with salt-bootstrap version >= 0.13.6 (you can find this information in the image catalog)");
    }

    @Test
    public void testRotateSaltPasswordFailure() throws Exception {
        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPassword("old-password");
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        when(securityConfigService.getOneByStackId(stack.getId())).thenReturn(securityConfig);

        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException("reason");
        doThrow(cause).when(hostOrchestrator).changePassword(any(), anyString(), eq(saltSecurityConfig.getSaltPassword()));

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isEqualTo(cause);

        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(hostOrchestrator).changePassword(any(), anyString(), anyString());
        verify(securityConfigService, never()).changeSaltPassword(eq(securityConfig), anyString());
    }

    @Test
    void sendSuccessUsageReport() {
        underTest.sendSuccessUsageReport(STACK_CRN, RotateSaltPasswordReason.MANUAL);

        verify(usageReporter).cdpSaltPasswordRotationEvent(eventArgumentCaptor.capture());
        UsageProto.CDPSaltPasswordRotationEvent event = eventArgumentCaptor.getValue();
        assertThat(event)
                .returns(STACK_CRN, UsageProto.CDPSaltPasswordRotationEvent::getResourceCrn)
                .returns(UsageProto.CDPSaltPasswordRotationEventReason.Value.MANUAL, UsageProto.CDPSaltPasswordRotationEvent::getReason)
                .returns(UsageProto.CDPSaltPasswordRotationEventResult.Value.SUCCESS, UsageProto.CDPSaltPasswordRotationEvent::getEventResult)
                .returns("", UsageProto.CDPSaltPasswordRotationEvent::getMessage);
    }

    @Test
    void sendFailureUsageReport() {
        String message = "failure message";

        underTest.sendFailureUsageReport(STACK_CRN, RotateSaltPasswordReason.EXPIRED, message);

        verify(usageReporter).cdpSaltPasswordRotationEvent(eventArgumentCaptor.capture());
        UsageProto.CDPSaltPasswordRotationEvent event = eventArgumentCaptor.getValue();
        assertThat(event)
                .returns(STACK_CRN, UsageProto.CDPSaltPasswordRotationEvent::getResourceCrn)
                .returns(UsageProto.CDPSaltPasswordRotationEventReason.Value.EXPIRED, UsageProto.CDPSaltPasswordRotationEvent::getReason)
                .returns(UsageProto.CDPSaltPasswordRotationEventResult.Value.FAILURE, UsageProto.CDPSaltPasswordRotationEvent::getEventResult)
                .returns(message, UsageProto.CDPSaltPasswordRotationEvent::getMessage);
    }

    @Test
    void expiredSaltPasswordRotationNeeded() throws Exception {
        when(clock.getCurrentLocalDateTime()).thenReturn(LocalDateTime.now());
        when(saltStatusCheckerConfig.getPasswordExpiryThresholdInDays()).thenReturn(14);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(hostOrchestrator.getPasswordExpiryDate(gatewayConfigs, RotateSaltPasswordService.SALTUSER)).thenReturn(LocalDate.now().minusMonths(2));

        Optional<RotateSaltPasswordReason> result = underTest.checkIfSaltPasswordRotationNeeded(stack);

        assertThat(result).hasValue(RotateSaltPasswordReason.EXPIRED);
    }

    @Test
    void noSaltPasswordRotationNeeded() throws Exception {
        when(clock.getCurrentLocalDateTime()).thenReturn(LocalDateTime.now());
        when(saltStatusCheckerConfig.getPasswordExpiryThresholdInDays()).thenReturn(14);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        when(hostOrchestrator.getPasswordExpiryDate(gatewayConfigs, RotateSaltPasswordService.SALTUSER)).thenReturn(LocalDate.now().plusMonths(2));

        Optional<RotateSaltPasswordReason> result = underTest.checkIfSaltPasswordRotationNeeded(stack);

        assertThat(result).isEmpty();
    }

    @Test
    void unauthorizedSaltPasswordRotationNeeded() throws Exception {
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        RuntimeException causeCause = new RuntimeException(RotateSaltPasswordService.UNAUTHORIZED_RESPONSE);
        RuntimeException cause = new RuntimeException("Ooops", causeCause);
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("Failed", cause);
        when(hostOrchestrator.getPasswordExpiryDate(gatewayConfigs, RotateSaltPasswordService.SALTUSER)).thenThrow(exception);

        Optional<RotateSaltPasswordReason> result = underTest.checkIfSaltPasswordRotationNeeded(stack);

        assertThat(result).hasValue(RotateSaltPasswordReason.UNAUTHORIZED);
    }

    @Test
    void errorWhileSaltPasswordRotationNeeded() throws Exception {
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(gatewayConfigs);
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("Unexpected failure");
        when(hostOrchestrator.getPasswordExpiryDate(gatewayConfigs, RotateSaltPasswordService.SALTUSER)).thenThrow(exception);

        assertThatThrownBy(() -> underTest.checkIfSaltPasswordRotationNeeded(stack))
                .isInstanceOf(CloudbreakRuntimeException.class)
                .hasCause(exception);
    }
}
