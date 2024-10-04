package com.sequenceiq.cloudbreak.service;

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

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordService;
import com.sequenceiq.cloudbreak.service.salt.RotateSaltPasswordValidator;
import com.sequenceiq.cloudbreak.service.salt.SaltPasswordStatusService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.usage.UsageReporter;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordServiceTest {

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:cloudera:datalake:33071a14-d605-4b2d-9a55-218c0dbc95e3";

    private static final String ACCOUNT_ID = "0";

    private static final String OLD_PASSWORD = "old-password";

    private static final String NEW_PASSWORD = "new-password";

    private static final String FQDN = "fqdn";

    @Mock
    private UsageReporter usageReporter;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private SecurityConfigService securityConfigService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private SaltPasswordStatusService saltPasswordStatusService;

    @Mock
    private StackDto stack;

    @Mock
    private InstanceMetadataView instanceMetadataView;

    @Mock
    private RotateSaltPasswordValidator rotateSaltPasswordValidator;

    @Captor
    private ArgumentCaptor<UsageProto.CDPSaltPasswordRotationEvent> eventArgumentCaptor;

    @Captor
    private ArgumentCaptor<StackDto> stackDtoArgumentCaptor;

    @InjectMocks
    private RotateSaltPasswordService underTest;

    private List<GatewayConfig> gatewayConfigs;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        lenient().when(stack.isAvailable()).thenReturn(true);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        lenient().when(stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(List.of(instanceMetadataView));
        lenient().when(entitlementService.isSaltUserPasswordRotationEnabled(ACCOUNT_ID)).thenReturn(true);

        GatewayConfig gw1 = new GatewayConfig("host1", "1.1.1.1", "1.1.1.1", 22, "i-1839", false);
        GatewayConfig gw2 = new GatewayConfig("host2", "1.1.1.2", "1.1.1.2", 22, "i-1839", false);
        gatewayConfigs = List.of(gw1, gw2);
        lenient().when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(gatewayConfigs);

        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPassword(OLD_PASSWORD);
        securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        lenient().when(stack.getSecurityConfig()).thenReturn(securityConfig);
        lenient().when(saltPasswordStatusService.getSaltPasswordStatus(stack)).thenReturn(SaltPasswordStatus.OK);
        Supplier<String> passwordGenerator = () -> NEW_PASSWORD;
        ReflectionTestUtils.setField(underTest, "passwordGenerator", passwordGenerator);

        Node node = mock(Node.class);
        lenient().when(node.getHostname()).thenReturn(FQDN);
        lenient().when(stack.getAllPrimaryGatewayInstanceNodes()).thenReturn(Set.of(node));
    }

    @Test
    public void testRotateSaltPasswordSuccess() throws Exception {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        underTest.rotateSaltPassword(stack);

        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(hostOrchestrator).changePassword(gatewayConfigs, NEW_PASSWORD, OLD_PASSWORD);
        verify(securityConfigService).changeSaltPassword(securityConfig, NEW_PASSWORD);
        verify(saltPasswordStatusService).getSaltPasswordStatus(stackDtoArgumentCaptor.capture());
        verifyCapturedStackSaltPassword();
    }

    private void verifyCapturedStackSaltPassword() {
        assertThat(stackDtoArgumentCaptor.getValue())
                .returns(NEW_PASSWORD, stackDto -> stackDto.getSecurityConfig().getSaltSecurityConfig().getSaltPassword());
    }

    @Test
    void rotateSaltPasswordStatusCheckFails() {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        when(saltPasswordStatusService.getSaltPasswordStatus(stack)).thenReturn(SaltPasswordStatus.INVALID);

        assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(CloudbreakOrchestratorFailedException.class)
                .hasMessage("Salt password status check failed with status INVALID, please try the operation again");
    }

    @Test
    public void testRotateSaltPasswordOnStackWithNotRunningInstanceAndDefaultImplementation() {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.STOPPED);
        lenient().when(stack.getNotTerminatedInstanceMetaData()).thenReturn(List.of(instanceMetaData));

        Assertions.assertThatCode(() -> underTest.rotateSaltPassword(stack))
                .doesNotThrowAnyException();
    }

    @Test
    public void testRotateSaltPasswordFailure() throws Exception {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        CloudbreakOrchestratorFailedException cause = new CloudbreakOrchestratorFailedException("reason");
        doThrow(cause).when(hostOrchestrator).changePassword(any(), anyString(), eq(securityConfig.getSaltSecurityConfig().getSaltPassword()));

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isEqualTo(cause);

        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(hostOrchestrator).changePassword(any(), anyString(), anyString());
        verify(securityConfigService, never()).changeSaltPassword(eq(securityConfig), anyString());
    }

    @Test
    void rotateSaltPasswordFallbackSuccess() throws Exception {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);
        underTest.rotateSaltPassword(stack);

        verify(hostOrchestrator).runCommandOnHosts(gatewayConfigs, Set.of(FQDN), "userdel saltuser");
        verify(clusterBootstrapper).reBootstrapGateways(stack);
        verify(securityConfigService).changeSaltPassword(securityConfig, NEW_PASSWORD);
        verify(saltPasswordStatusService).getSaltPasswordStatus(stackDtoArgumentCaptor.capture());
        verifyCapturedStackSaltPassword();
    }

    @Test
    void rotateSaltPasswordFallbackUserDeleteFails() throws Exception {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);
        when(hostOrchestrator.runCommandOnHosts(gatewayConfigs, Set.of(FQDN), "userdel saltuser")).thenThrow(CloudbreakOrchestratorFailedException.class);

        underTest.rotateSaltPassword(stack);

        String newPassword = stack.getSecurityConfig().getSaltSecurityConfig().getSaltPassword();
        assertThat(newPassword).isNotEqualTo(OLD_PASSWORD);
        verify(hostOrchestrator).runCommandOnHosts(gatewayConfigs, Set.of(FQDN), "userdel saltuser");
        verify(clusterBootstrapper).reBootstrapGateways(stack);
        verify(securityConfigService).changeSaltPassword(securityConfig, newPassword);
    }

    @Test
    void rotateSaltPasswordFallbackFailure() throws Exception {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);
        doThrow(CloudbreakException.class).when(clusterBootstrapper).reBootstrapGateways(stack);

        assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(CloudbreakOrchestratorFailedException.class)
                .hasCauseInstanceOf(CloudbreakException.class)
                .hasMessage("Failed to re-bootstrap gateway nodes after saltuser password delete. " +
                        "Please check the salt-bootstrap service status on node(s) [1.1.1.1, 1.1.1.2]. " +
                        "If the saltuser password was changed manually, " +
                        "please remove the user manually with the command 'userdel saltuser' on node(s) [1.1.1.1, 1.1.1.2] and retry the operation.");

        verify(securityConfigService, never()).changeSaltPassword(eq(securityConfig), any());
    }

    @Test
    void rotateSaltPasswordFallbackStatusCheckFails() {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);
        when(saltPasswordStatusService.getSaltPasswordStatus(stack)).thenReturn(SaltPasswordStatus.INVALID);

        assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isInstanceOf(CloudbreakOrchestratorFailedException.class)
                .hasMessage("Failed to re-bootstrap gateway nodes after saltuser password delete. " +
                        "Please check the salt-bootstrap service status on node(s) [1.1.1.1, 1.1.1.2]. " +
                        "If the saltuser password was changed manually, " +
                        "please remove the user manually with the command 'userdel saltuser' on node(s) [1.1.1.1, 1.1.1.2] and retry the operation.")
                .hasCause(new CloudbreakOrchestratorFailedException("Salt password status check failed with status INVALID, please try the operation again"));
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
}
