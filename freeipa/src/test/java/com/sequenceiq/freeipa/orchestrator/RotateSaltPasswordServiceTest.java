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

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.quartz.saltstatuschecker.SaltStatusCheckerConfig;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.dto.RotateSaltPasswordReason;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.rotation.FreeIpaSecretType;
import com.sequenceiq.freeipa.service.BootstrapService;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.rotation.FreeIpaSecretRotationService;
import com.sequenceiq.freeipa.util.SaltBootstrapVersionChecker;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:cloudera:environment:6a6617d8-0ffb-4e33-90d1-36b33e62fb3c";

    private static final String ACCOUNT_ID = "0";

    private static final String OLD_PASSWORD = "old-password";

    private static final long STACK_ID = 1L;

    private static final String FQDN = "fqdn";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private SaltBootstrapVersionChecker saltBootstrapVersionChecker;

    @Mock
    private BootstrapService bootstrapService;

    @Mock
    private FreeIpaSecretRotationService freeIpaSecretRotationService;

    @Mock
    private Clock clock;

    @Mock
    private SaltStatusCheckerConfig saltStatusCheckerConfig;

    @Mock
    private Stack stack;

    @Mock
    private GatewayConfig gatewayConfig;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @InjectMocks
    private RotateSaltPasswordService underTest;

    @Captor
    private ArgumentCaptor<FreeIpaSecretRotationRequest> requestCaptor;

    @BeforeEach
    void setUp() {
        lenient().when(gatewayConfig.getPrivateAddress()).thenReturn("8.8.8.8");

        lenient().when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        lenient().when(gatewayConfigService.getNotDeletedGatewayConfigs(stack)).thenReturn(List.of(gatewayConfig));

        lenient().when(stack.getId()).thenReturn(STACK_ID);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.isStopped()).thenReturn(false);
        Node node = mock(Node.class);
        lenient().when(node.getHostname()).thenReturn(FQDN);
        lenient().when(stack.getAllFunctioningNodes()).thenReturn(Set.of(node));

        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPasswordVault(OLD_PASSWORD);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        lenient().when(stack.getSecurityConfig()).thenReturn(securityConfig);
        lenient().when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret("new-password", OLD_PASSWORD));
    }

    @Test
    void rotateSaltPasswordFallback() throws Exception {
        when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);
        setPasswordExpiry(LocalDate.now().plusMonths(2));

        underTest.rotateSaltPassword(stack);

        verify(hostOrchestrator).runCommandOnHosts(List.of(gatewayConfig), Set.of(FQDN), SALTUSER_DELETE_COMMAND);
        verify(bootstrapService).reBootstrap(stack);
    }

    @Test
    void rotateSaltPasswordFallbackWithFailedUserDelete() throws Exception {
        when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);
        when(hostOrchestrator.runCommandOnHosts(List.of(gatewayConfig), Set.of(FQDN), SALTUSER_DELETE_COMMAND))
                .thenThrow(CloudbreakOrchestratorFailedException.class);
        setPasswordExpiry(LocalDate.now().plusMonths(2));

        underTest.rotateSaltPassword(stack);

        verify(hostOrchestrator).runCommandOnHosts(List.of(gatewayConfig), Set.of(FQDN), SALTUSER_DELETE_COMMAND);
        verify(bootstrapService).reBootstrap(stack);
    }

    @Test
    void rotateSaltPasswordFallbackWithFailedReBootstrap() throws Exception {
        when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException("");
        doThrow(cause).when(bootstrapService).reBootstrap(stack);

        assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(SecretRotationException.class)
                .hasCause(cause)
                .hasMessage("Failed to re-bootstrap gateway nodes after saltuser password delete. " +
                        "Please check the salt-bootstrap service status on node(s) %s. " +
                        "If the saltuser password was changed manually, " +
                        "please remove the user manually with the command '%s' on node(s) %s and retry the operation.",
                        List.of("8.8.8.8"), SALTUSER_DELETE_COMMAND, List.of("8.8.8.8"));

        verify(hostOrchestrator).runCommandOnHosts(List.of(gatewayConfig), Set.of(FQDN), SALTUSER_DELETE_COMMAND);
        verify(bootstrapService).reBootstrap(stack);
    }

    @Test
    void rotateSaltPasswordFailedOrchestrator() throws CloudbreakOrchestratorException {
        when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        CloudbreakOrchestratorFailedException orchestratorFailedException = new CloudbreakOrchestratorFailedException("message");
        doThrow(orchestratorFailedException).when(hostOrchestrator).changePassword(any(), any(), any());

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(SecretRotationException.class)
                .hasCause(orchestratorFailedException)
                .hasMessage(orchestratorFailedException.getMessage());
    }

    @Test
    void rotateSaltPasswordSuccess() throws CloudbreakOrchestratorException {
        when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        setPasswordExpiry(LocalDate.now().plusMonths(2));

        underTest.rotateSaltPassword(stack);

        verify(hostOrchestrator).changePassword(eq(List.of(gatewayConfig)), anyString(), eq(OLD_PASSWORD));
    }

    @Test
    void triggerRotateSaltPassword() {
        FlowIdentifier flowIdentifier = new FlowIdentifier(FlowType.FLOW, "pollable-id");

        FlowIdentifier result = underTest.triggerRotateSaltPassword(ENVIRONMENT_CRN, ACCOUNT_ID, RotateSaltPasswordReason.MANUAL);

        verify(freeIpaSecretRotationService).rotateSecretsByCrn(eq(ACCOUNT_ID), eq(ENVIRONMENT_CRN), requestCaptor.capture());
        FreeIpaSecretRotationRequest request = requestCaptor.getValue();
        assertThat(request.getSecrets()).containsOnly(FreeIpaSecretType.SALT_PASSWORD.value());
    }

    @Test
    void expiredSaltPasswordRotationNeeded() throws Exception {
        setPasswordExpiry(LocalDate.now().minusMonths(2));

        Optional<RotateSaltPasswordReason> result = underTest.checkIfSaltPasswordRotationNeeded(stack);

        assertThat(result).hasValue(RotateSaltPasswordReason.EXPIRED);
    }

    @Test
    void noSaltPasswordRotationNeeded() throws Exception {
        setPasswordExpiry(LocalDate.now().plusMonths(2));

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

    private void setPasswordExpiry(LocalDate t) throws CloudbreakOrchestratorException {
        lenient().when(clock.getCurrentLocalDateTime()).thenReturn(LocalDateTime.now());
        lenient().when(saltStatusCheckerConfig.getPasswordExpiryThresholdInDays()).thenReturn(14);
        lenient().when(hostOrchestrator.getPasswordExpiryDate(List.of(gatewayConfig), RotateSaltPasswordService.SALTUSER)).thenReturn(t);
    }

}
