package com.sequenceiq.cloudbreak.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
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
import com.sequenceiq.cloudbreak.service.secret.domain.RotationSecret;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class RotateSaltPasswordServiceTest {

    private static final String STACK_CRN = "crn:cdp:datalake:us-west-1:cloudera:datalake:33071a14-d605-4b2d-9a55-218c0dbc95e3";

    private static final String ACCOUNT_ID = "0";

    private static final String OLD_PASSWORD = "old-password";

    private static final String NEW_PASSWORD = "new-password";

    private static final String FQDN = "fqdn";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private GatewayConfigService gatewayConfigService;

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

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @Captor
    private ArgumentCaptor<StackDto> stackDtoArgumentCaptor;

    @InjectMocks
    private RotateSaltPasswordService underTest;

    private List<GatewayConfig> gatewayConfigs;

    @BeforeEach
    void setUp() {
        lenient().when(stack.isAvailable()).thenReturn(true);
        lenient().when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        lenient().when(stack.getStatus()).thenReturn(Status.AVAILABLE);
        lenient().when(stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata()).thenReturn(List.of(instanceMetadataView));

        GatewayConfig gw1 = GatewayConfig.builder()
                .withConnectionAddress("host1")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build();
        GatewayConfig gw2 = GatewayConfig.builder()
                .withConnectionAddress("host2")
                .withPublicAddress("1.1.1.2")
                .withPrivateAddress("1.1.1.2")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build();
        gatewayConfigs = List.of(gw1, gw2);
        lenient().when(gatewayConfigService.getAllGatewayConfigs(any())).thenReturn(gatewayConfigs);

        SaltSecurityConfig saltSecurityConfig = new SaltSecurityConfig();
        saltSecurityConfig.setSaltPassword(OLD_PASSWORD);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setSaltSecurityConfig(saltSecurityConfig);
        lenient().when(stack.getSecurityConfig()).thenReturn(securityConfig);
        lenient().when(saltPasswordStatusService.getSaltPasswordStatus(stack)).thenReturn(SaltPasswordStatus.OK);

        Node node = mock(Node.class);
        lenient().when(node.getHostname()).thenReturn(FQDN);
        lenient().when(stack.getAllPrimaryGatewayInstanceNodes()).thenReturn(Set.of(node));
        lenient().when(uncachedSecretServiceForRotation.getRotation(any())).thenReturn(new RotationSecret(NEW_PASSWORD, OLD_PASSWORD));
    }

    @Test
    public void testRotateSaltPasswordSuccess() throws Exception {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(true);
        underTest.rotateSaltPassword(stack);

        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(hostOrchestrator).changePassword(gatewayConfigs, NEW_PASSWORD, OLD_PASSWORD);
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
        doThrow(cause).when(hostOrchestrator).changePassword(any(), anyString(), anyString());

        Assertions.assertThatThrownBy(() -> underTest.rotateSaltPassword(stack))
                .isEqualTo(cause);

        verify(gatewayConfigService).getAllGatewayConfigs(stack);
        verify(hostOrchestrator).changePassword(any(), anyString(), anyString());
    }

    @Test
    void rotateSaltPasswordFallbackSuccess() throws Exception {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);
        underTest.rotateSaltPassword(stack);

        verify(hostOrchestrator).runCommandOnHosts(gatewayConfigs, Set.of(FQDN), "userdel saltuser");
        verify(clusterBootstrapper).reBootstrapGateways(stack);
    }

    @Test
    void rotateSaltPasswordFallbackUserDeleteFails() throws Exception {
        when(rotateSaltPasswordValidator.isChangeSaltuserPasswordSupported(stack)).thenReturn(false);
        when(hostOrchestrator.runCommandOnHosts(gatewayConfigs, Set.of(FQDN), "userdel saltuser")).thenThrow(CloudbreakOrchestratorFailedException.class);

        underTest.rotateSaltPassword(stack);

        verify(hostOrchestrator).runCommandOnHosts(gatewayConfigs, Set.of(FQDN), "userdel saltuser");
        verify(clusterBootstrapper).reBootstrapGateways(stack);
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
    }
}
