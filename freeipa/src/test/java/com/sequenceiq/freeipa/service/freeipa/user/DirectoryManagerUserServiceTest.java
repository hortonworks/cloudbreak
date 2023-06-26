package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@ExtendWith(MockitoExtension.class)
class DirectoryManagerUserServiceTest {

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private FreeIpaService freeIpaService;

    @InjectMocks
    private DirectoryManagerUserService directoryManagerUserService;

    @Test
    public void updateDirectoryManagerPasswordFailesOnOneHostTest() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        InstanceMetaData primaryGWInstanceMetadata = mock(InstanceMetaData.class);
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(primaryGWInstanceMetadata));
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getGatewayConfig(stack, primaryGWInstanceMetadata)).thenReturn(gatewayConfig);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword("adminpassword");
        ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
        when(hostOrchestrator.runCommandOnAllHosts(eq(gatewayConfig), commandCaptor.capture())).thenReturn(Map.of("host1", "Successfully replaced",
                "host2", "failed"));
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);

        CloudbreakRuntimeException cloudbreakRuntimeException = assertThrows(CloudbreakRuntimeException.class,
                () -> directoryManagerUserService.updateDirectoryManagerPassword(stack, "newpassword"));
        assertEquals("Directory Manager password change failed on host2. {host2=failed}", cloudbreakRuntimeException.getMessage());
    }

    @Test
    public void updateDirectoryManagerPasswordTest() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        InstanceMetaData primaryGWInstanceMetadata = mock(InstanceMetaData.class);
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(primaryGWInstanceMetadata));
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getGatewayConfig(stack, primaryGWInstanceMetadata)).thenReturn(gatewayConfig);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword("adminpassword");
        ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
        when(hostOrchestrator.runCommandOnAllHosts(eq(gatewayConfig), commandCaptor.capture())).thenReturn(Map.of("host1", "Successfully replaced",
                "host2", "Successfully replaced"));
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        directoryManagerUserService.updateDirectoryManagerPassword(stack, "newpassword");
        assertEquals("export HOSTNAME=$(hostname -f);dsconf -D \"cn=Directory Manager\" -w \"adminpassword\" " +
                        "ldaps://$HOSTNAME config replace nsslapd-rootpw=\"newpassword\"",
                commandCaptor.getValue());
    }

    @Test
    public void checkDirectoryManagerPasswordTest() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        InstanceMetaData primaryGWInstanceMetadata = mock(InstanceMetaData.class);
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(primaryGWInstanceMetadata));
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getGatewayConfig(stack, primaryGWInstanceMetadata)).thenReturn(gatewayConfig);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword("adminpassword");
        ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
        when(hostOrchestrator.runCommandOnAllHosts(eq(gatewayConfig), commandCaptor.capture())).thenReturn(Map.of("host1", "nsslapd-port: 389",
                "host2", "nsslapd-port: 389"));
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        directoryManagerUserService.checkDirectoryManagerPassword(stack);
        assertEquals("export HOSTNAME=$(hostname -f);dsconf -D \"cn=Directory Manager\" -w \"adminpassword\" ldaps://$HOSTNAME config get",
                commandCaptor.getValue());
    }

    @Test
    public void checkDirectoryManagerPasswordFailedTest() throws CloudbreakOrchestratorFailedException {
        Stack stack = mock(Stack.class);
        InstanceMetaData primaryGWInstanceMetadata = mock(InstanceMetaData.class);
        when(stack.getPrimaryGateway()).thenReturn(Optional.of(primaryGWInstanceMetadata));
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        when(gatewayConfigService.getGatewayConfig(stack, primaryGWInstanceMetadata)).thenReturn(gatewayConfig);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword("adminpassword");
        ArgumentCaptor<String> commandCaptor = ArgumentCaptor.forClass(String.class);
        when(hostOrchestrator.runCommandOnAllHosts(eq(gatewayConfig), commandCaptor.capture())).thenReturn(Map.of("host1", "nsslapd-port: 389",
                "host2", "failed"));
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        CloudbreakRuntimeException cloudbreakRuntimeException = assertThrows(CloudbreakRuntimeException.class,
                () -> directoryManagerUserService.checkDirectoryManagerPassword(stack));
        assertEquals("Directory Manager password check failed on host2. {host2=failed}", cloudbreakRuntimeException.getMessage());
        assertEquals("export HOSTNAME=$(hostname -f);dsconf -D \"cn=Directory Manager\" -w \"adminpassword\" ldaps://$HOSTNAME config get",
                commandCaptor.getValue());
    }
}