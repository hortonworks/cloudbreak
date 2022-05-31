package com.sequenceiq.freeipa.orchestrator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.SecurityConfigService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.SaltBootstrapVersionChecker;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:cloudera:environment:6a6617d8-0ffb-4e33-90d1-36b33e62fb3c";

    private static final String ACCOUNT_ID = "0";

    private static final String OLD_PASSWORD = "old-password";

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
    private Stack stack;

    @Mock
    private GatewayConfig gatewayConfig;

    @InjectMocks
    private RotateSaltPasswordService underTest;

    @BeforeEach
    void setUp() {
        lenient().when(entitlementService.isSaltUserPasswordRotationEnabled(ACCOUNT_ID)).thenReturn(true);
        lenient().when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        lenient().when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        lenient().when(gatewayConfigService.getNotDeletedGatewayConfigs(stack)).thenReturn(List.of(gatewayConfig));

        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPassword(OLD_PASSWORD);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        lenient().when(stack.getSecurityConfig()).thenReturn(securityConfig);
    }

    @Test
    void rotateSaltPasswordWithoutEntitlement() {
        when(entitlementService.isSaltUserPasswordRotationEnabled(ACCOUNT_ID)).thenReturn(false);

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(ENVIRONMENT_CRN, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Rotating salt password is not supported in your account");
    }

    @Test
    void rotateSaltPasswordWithOldSBVersion() {
        when(saltBootstrapVersionChecker.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(ENVIRONMENT_CRN, ACCOUNT_ID))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Rotating salt password is not supported with your image version, " +
                        "please upgrade to an image with salt-bootstrap version >= 0.13.6 (you can find this information in the image catalog)");
    }

    @Test
    void rotateSaltPasswordFailedOrchestrator() throws CloudbreakOrchestratorException {
        CloudbreakOrchestratorFailedException orchestratorFailedException = new CloudbreakOrchestratorFailedException("message");
        doThrow(orchestratorFailedException).when(hostOrchestrator).changePassword(any(), any(), any());

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(ENVIRONMENT_CRN, ACCOUNT_ID))
                .isInstanceOf(CloudbreakServiceException.class)
                .hasCause(orchestratorFailedException)
                .hasMessage(orchestratorFailedException.getMessage());

        verify(securityConfigService, never()).changeSaltPassword(eq(stack), anyString());
    }

    @Test
    void rotateSaltPasswordSuccess() throws CloudbreakOrchestratorException {
        underTest.rotateSaltPassword(ENVIRONMENT_CRN, ACCOUNT_ID);

        verify(hostOrchestrator).changePassword(eq(List.of(gatewayConfig)), anyString(), eq(OLD_PASSWORD));
        verify(securityConfigService).changeSaltPassword(eq(stack), anyString());
    }

}
