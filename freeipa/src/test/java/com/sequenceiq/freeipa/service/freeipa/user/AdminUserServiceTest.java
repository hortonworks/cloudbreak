package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory.ADMIN_USER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.PasswordPolicy;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaBackupConfigView;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Mock
    private FreeIpaConfigService freeIpaConfigService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    public void testUpdateAdminUserPasswordUpdatePasswordPolicy() throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        passwordPolicy.setKrbminpwdlife(10);
        when(freeIpaClient.getPasswordPolicy()).thenReturn(passwordPolicy);
        String newPassword = "newpassword";
        adminUserService.updateAdminUserPassword(newPassword, freeIpaClient);
        verify(freeIpaClient, times(1)).updatePasswordPolicy(Map.of("krbminpwdlife", 0L));
        freeIpaClient.userSetPasswordWithExpiration(ADMIN_USER, newPassword, Optional.empty());
    }

    @Test
    public void testUpdateAdminUserPasswordDontUpdatePasswordPolicy() throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        PasswordPolicy passwordPolicy = new PasswordPolicy();
        passwordPolicy.setKrbminpwdlife(0);
        when(freeIpaClient.getPasswordPolicy()).thenReturn(passwordPolicy);
        String newPassword = "newpassword";
        adminUserService.updateAdminUserPassword(newPassword, freeIpaClient);
        verify(freeIpaClient, times(0)).updatePasswordPolicy(any());
        freeIpaClient.userSetPasswordWithExpiration(ADMIN_USER, newPassword, Optional.empty());
    }

    @Test
    public void testUpdatePillar() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        InstanceMetaData primaryGWInstanceMetadata = mock(InstanceMetaData.class);
        InstanceMetaData otherInstanceMetadata = mock(InstanceMetaData.class);
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(primaryGWInstanceMetadata));
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(primaryGWInstanceMetadata, otherInstanceMetadata);
        when(stack.getNotDeletedInstanceMetaDataSet()).thenReturn(instanceMetaDataSet);
        Node node1 = mock(Node.class);
        when(node1.getHostname()).thenReturn("host1");
        Node node2 = mock(Node.class);
        when(node2.getHostname()).thenReturn("host2");
        Set<Node> allNodes = Set.of(node1, node2);
        when(freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDataSet)).thenReturn(allNodes);
        FreeIpaConfigView freeIpaConfigView = new FreeIpaConfigView.Builder()
                .withAdminUser("adminuser")
                .withPassword("adminpassword")
                .withBackupConfig(mock(FreeIpaBackupConfigView.class))
                .build();
        when(freeIpaConfigService.createFreeIpaConfigs(stack, allNodes)).thenReturn(freeIpaConfigView);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getGatewayConfig(stack, primaryGWInstanceMetadata)).thenReturn(gatewayConfig);
        adminUserService.updateFreeIpaPillar(stack);
        ArgumentCaptor<SaltConfig> saltConfigArgumentCaptor = ArgumentCaptor.forClass(SaltConfig.class);
        ArgumentCaptor<OrchestratorStateParams> orchestratorStateParamsArgumentCaptor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator, times(1)).saveCustomPillars(saltConfigArgumentCaptor.capture(), eq(null), orchestratorStateParamsArgumentCaptor.capture());
        SaltConfig saltConfig = saltConfigArgumentCaptor.getValue();
        Map<String, SaltPillarProperties> servicePillarConfig = saltConfig.getServicePillarConfig();
        SaltPillarProperties freeipaPillarProperties = servicePillarConfig.get("freeipa");
        assertEquals("adminuser", ((Map<String, String>) freeipaPillarProperties.getProperties().get("freeipa")).get("admin_user"));
        assertEquals("adminpassword", ((Map<String, String>) freeipaPillarProperties.getProperties().get("freeipa")).get("password"));
    }
}