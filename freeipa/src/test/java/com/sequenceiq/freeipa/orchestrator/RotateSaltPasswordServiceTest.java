package com.sequenceiq.freeipa.orchestrator;

import static com.sequenceiq.freeipa.orchestrator.RotateSaltPasswordService.SALTUSER_DELETE_COMMAND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.RotateSaltPasswordEvent;
import com.sequenceiq.freeipa.flow.freeipa.salt.rotatepassword.event.RotateSaltPasswordReason;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.SaltBootstrapVersionChecker;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:cloudera:environment:6a6617d8-0ffb-4e33-90d1-36b33e62fb3c";

    private static final String ACCOUNT_ID = "0";

    private static final String OLD_PASSWORD = "old-password";

    private static final long STACK_ID = 1L;

    private static final String FQDN = "fqdn";

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Mock
    private BootstrapService bootstrapService;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private Clock clock;

    @Mock
    private SaltStatusCheckerConfig saltStatusCheckerConfig;

    @Mock
    private Stack stack;

    @Mock
    private GatewayConfig gatewayConfig;

    @InjectMocks
    private RotateSaltPasswordService underTest;

    @Captor
    private ArgumentCaptor<StackEvent> stackEventArgumentCaptor;

    @Captor
    private ArgumentCaptor<UsageProto.CDPSaltPasswordRotationEvent> eventArgumentCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(gatewayConfig.getPrivateAddress()).thenReturn("8.8.8.8");

        lenient().when(entitlementService.isSaltUserPasswordRotationEnabled(ACCOUNT_ID)).thenReturn(true);
        lenient().when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        lenient().when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        lenient().when(gatewayConfigService.getNotDeletedGatewayConfigs(stack)).thenReturn(List.of(gatewayConfig));

        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.isStopped()).thenReturn(false);
        Node node = mock(Node.class);
        lenient().when(node.getHostname()).thenReturn(FQDN);
        lenient().when(stack.getAllFunctioningNodes()).thenReturn(Set.of(node));

        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPassword(OLD_PASSWORD);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        lenient().when(stack.getSecurityConfig()).thenReturn(securityConfig);
    }

    @Test
    void  rotateSaltPasswordForStoppedStack() {
        lenient().when(stack.isStopped()).thenReturn(true);

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Rotating SaltStack user password is not supported for stopped clusters");
    }

    @Test
    void rotateSaltPasswordWithoutEntitlement() {
        when(entitlementService.isSaltUserPasswordRotationEnabled(ACCOUNT_ID)).thenReturn(false);

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Rotating SaltStack user password is not supported in your account");
    }

    @Test
    void rotateSaltPasswordFallback() throws Exception {
        underTest.rotateSaltPasswordFallback(stack);

        verify(hostOrchestrator).runCommandOnHosts(List.of(gatewayConfig), Set.of(FQDN), SALTUSER_DELETE_COMMAND);
        verify(bootstrapService).reBootstrap(stack);
        verify(securityConfigService).changeSaltPassword(eq(stack), anyString());
    }

    @Test
    void rotateSaltPasswordFallbackWithFailedUserDelete() throws Exception {
        when(hostOrchestrator.runCommandOnHosts(List.of(gatewayConfig), Set.of(FQDN), SALTUSER_DELETE_COMMAND))
                .thenThrow(CloudbreakOrchestratorFailedException.class);

        underTest.rotateSaltPasswordFallback(stack);

        verify(hostOrchestrator).runCommandOnHosts(List.of(gatewayConfig), Set.of(FQDN), SALTUSER_DELETE_COMMAND);
        verify(bootstrapService).reBootstrap(stack);
        verify(securityConfigService).changeSaltPassword(eq(stack), anyString());
    }

    @Test
    void rotateSaltPasswordFallbackWithFailedReBootstrap() throws Exception {
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException("");
        doThrow(cause).when(bootstrapService).reBootstrap(stack);

        assertThatThrownBy(() -> underTest.rotateSaltPasswordFallback(stack))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasCause(cause)
                .hasMessage("Failed to re-bootstrap gateway nodes after saltuser password delete. " +
                        "Please check the salt-bootstrap service status on node(s) %s. " +
                        "If the saltuser password was changed manually, " +
                        "please remove the user manually with the command '%s' on node(s) %s and retry the operation.",
                        List.of("8.8.8.8"), SALTUSER_DELETE_COMMAND, List.of("8.8.8.8"));

        verify(hostOrchestrator).runCommandOnHosts(List.of(gatewayConfig), Set.of(FQDN), SALTUSER_DELETE_COMMAND);
        verify(bootstrapService).reBootstrap(stack);
        verify(securityConfigService, never()).changeSaltPassword(eq(stack), anyString());
    }

    @Test
    void rotateSaltPasswordFailedOrchestrator() throws CloudbreakOrchestratorException {
        CloudbreakOrchestratorFailedException orchestratorFailedException = new CloudbreakOrchestratorFailedException("message");
        doThrow(orchestratorFailedException).when(hostOrchestrator).changePassword(any(), any(), any());

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasCause(orchestratorFailedException)
                .hasMessage(orchestratorFailedException.getMessage());

        verify(securityConfigService, never()).changeSaltPassword(eq(stack), anyString());
    }

    @Test
    void rotateSaltPasswordSuccess() throws CloudbreakOrchestratorException {
        underTest.rotateSaltPassword(stack);

        verify(hostOrchestrator).changePassword(eq(List.of(gatewayConfig)), anyString(), eq(OLD_PASSWORD));
        verify(securityConfigService).changeSaltPassword(eq(stack), anyString());
    }

    @Test
    void triggerRotateSaltPassword() {
        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollable-id");
        when(flowManager.notify(anyString(), any())).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.triggerRotateSaltPassword(ENVIRONMENT_CRN, ACCOUNT_ID, RotateSaltPasswordReason.MANUAL);

        assertThat(result)
                .isEqualTo(flowIdentifier);
        String selector = RotateSaltPasswordEvent.ROTATE_SALT_PASSWORD_EVENT.event();
        verify(flowManager).notify(eq(selector), stackEventArgumentCaptor.capture());
        StackEvent stackEvent = stackEventArgumentCaptor.getValue();
        assertThat(stackEvent)
                .returns(stack.getId(), StackEvent::getResourceId);
    }

    @Test
    void sendSuccessUsageReport() {
        underTest.sendSuccessUsageReport(ENVIRONMENT_CRN, RotateSaltPasswordReason.MANUAL);

        verify(usageReporter).cdpSaltPasswordRotationEvent(eventArgumentCaptor.capture());
        UsageProto.CDPSaltPasswordRotationEvent event = eventArgumentCaptor.getValue();
        assertThat(event)
                .returns(ENVIRONMENT_CRN, UsageProto.CDPSaltPasswordRotationEvent::getResourceCrn)
                .returns(UsageProto.CDPSaltPasswordRotationEventReason.Value.MANUAL, UsageProto.CDPSaltPasswordRotationEvent::getReason)
                .returns(UsageProto.CDPSaltPasswordRotationEventResult.Value.SUCCESS, UsageProto.CDPSaltPasswordRotationEvent::getEventResult)
                .returns("", UsageProto.CDPSaltPasswordRotationEvent::getMessage);
    }

    @Test
    void sendFailureUsageReport() {
        String message = "failure message";
        underTest.sendFailureUsageReport(ENVIRONMENT_CRN, RotateSaltPasswordReason.EXPIRED, message);

        verify(usageReporter).cdpSaltPasswordRotationEvent(eventArgumentCaptor.capture());
        UsageProto.CDPSaltPasswordRotationEvent event = eventArgumentCaptor.getValue();
        assertThat(event)
                .returns(ENVIRONMENT_CRN, UsageProto.CDPSaltPasswordRotationEvent::getResourceCrn)
                .returns(UsageProto.CDPSaltPasswordRotationEventReason.Value.EXPIRED, UsageProto.CDPSaltPasswordRotationEvent::getReason)
                .returns(UsageProto.CDPSaltPasswordRotationEventResult.Value.FAILURE, UsageProto.CDPSaltPasswordRotationEvent::getEventResult)
                .returns(message, UsageProto.CDPSaltPasswordRotationEvent::getMessage);
    }

    @Test
    void expiredSaltPasswordRotationNeeded() throws Exception {
        when(clock.getCurrentLocalDateTime()).thenReturn(LocalDateTime.now());
        when(saltStatusCheckerConfig.getPasswordExpiryThresholdInDays()).thenReturn(14);
        when(hostOrchestrator.getPasswordExpiryDate(List.of(gatewayConfig), RotateSaltPasswordService.SALTUSER)).thenReturn(LocalDate.now().minusMonths(2));

        Optional<RotateSaltPasswordReason> result = underTest.checkIfSaltPasswordRotationNeeded(stack);

        assertThat(result).hasValue(RotateSaltPasswordReason.EXPIRED);
    }

    @Test
    void noSaltPasswordRotationNeeded() throws Exception {
        when(clock.getCurrentLocalDateTime()).thenReturn(LocalDateTime.now());
        when(saltStatusCheckerConfig.getPasswordExpiryThresholdInDays()).thenReturn(14);
        when(hostOrchestrator.getPasswordExpiryDate(List.of(gatewayConfig), RotateSaltPasswordService.SALTUSER)).thenReturn(LocalDate.now().plusMonths(2));

        Optional<RotateSaltPasswordReason> result = underTest.checkIfSaltPasswordRotationNeeded(stack);

        assertThat(result).isEmpty();
    }

    @Test
    void unauthorizedSaltPasswordRotationNeeded() throws Exception {
        RuntimeException causeCause = new RuntimeException(RotateSaltPasswordService.UNAUTHORIZED_RESPONSE);
        RuntimeException cause = new RuntimeException("Ooops", causeCause);
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("Failed", cause);
        when(hostOrchestrator.getPasswordExpiryDate(List.of(gatewayConfig), RotateSaltPasswordService.SALTUSER)).thenThrow(exception);

        Optional<RotateSaltPasswordReason> result = underTest.checkIfSaltPasswordRotationNeeded(stack);

        assertThat(result).hasValue(RotateSaltPasswordReason.UNAUTHORIZED);
    }

    @Test
    void errorWhileSaltPasswordRotationNeeded() throws Exception {
        CloudbreakOrchestratorFailedException exception = new CloudbreakOrchestratorFailedException("Unexpected failure");
        when(hostOrchestrator.getPasswordExpiryDate(List.of(gatewayConfig), RotateSaltPasswordService.SALTUSER)).thenThrow(exception);

        assertThatThrownBy(() -> underTest.checkIfSaltPasswordRotationNeeded(stack))
                .isInstanceOf(CloudbreakRuntimeException.class)
                .hasCause(exception);
    }

}
