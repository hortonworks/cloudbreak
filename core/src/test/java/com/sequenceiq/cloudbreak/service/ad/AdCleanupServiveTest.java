package com.sequenceiq.cloudbreak.service.ad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;

@ExtendWith(MockitoExtension.class)
class AdCleanupServiceTest {

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String STACK_NAME = "stack name";

    private static final String CLOUD_PLATFORM = "cloudPlatform";

    private static final String AD_CLEANUP_NODES = "ad-cleanup-nodes";

    private static final String KERBEROS = "kerberos";

    private static final String LDAP = "ldap";

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @InjectMocks
    private AdCleanupService underTest;

    @Test
    void testCleanUpAdWithHostnames() throws Exception {
        // Arrange
        String host1 = "master0.example.com";
        String host2 = "worker0.example.com";
        Set<String> hostnames = Set.of(host1, host2);
        StackDtoDelegate stack = aStack();
        GatewayConfig mockGatewayConfig = GatewayConfig.builder().withHostname("master0.example.com").build();
        KerberosConfig mockKerberosConfig = mock(KerberosConfig.class);
        LdapView mockLdapView = mock(LdapView.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(mockGatewayConfig);
        when(kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName())).thenReturn(Optional.of(mockKerberosConfig));
        when(ldapConfigService.get(stack.getEnvironmentCrn(), stack.getName())).thenReturn(Optional.of(mockLdapView));

        // Act
        List<String> errors = underTest.cleanUpAd(hostnames, stack);

        // Assert
        assertTrue(errors.isEmpty());
        ArgumentCaptor<OrchestratorStateParams> captor = ArgumentCaptor.forClass(OrchestratorStateParams.class);
        verify(hostOrchestrator, times(1)).runOrchestratorState(captor.capture());

        OrchestratorStateParams capturedParams = captor.getValue();
        // Perform assertions on capturedParams
        assertNotNull(capturedParams);
        assertEquals("sssd/ad-cleanup", capturedParams.getState());
        assertEquals(Set.of(host1), capturedParams.getTargetHostNames());
        assertNotNull(capturedParams.getStateParams().get(LDAP));
        assertNotNull(capturedParams.getStateParams().get(KERBEROS));
        assertEquals(hostnames, Set.of(((String) ((Map) capturedParams.getStateParams().get(AD_CLEANUP_NODES)).get("all_hostnames")).split(" ")));
    }

    @Test
    void testCleanUpAdNoKerberosConfigReturnsError() throws Exception {
        // Arrange
        Set<String> hostnames = Set.of("master0.example.com", "worker0.example.com");
        StackDtoDelegate stack = aStack();
        OrchestratorStateParams params = mock(OrchestratorStateParams.class);
        GatewayConfig mockGatewayConfig = GatewayConfig.builder().withHostname("master0.example.com").build();
//        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(mockGatewayConfig);

        // Act
        List<String> errors = underTest.cleanUpAd(hostnames, stack);

        // Assert
        assertEquals("AD Cleanup failed. Kerberos config not found.", errors.getFirst());
    }

    @Test
    void testCleanUpAdNoLdapConfigReturnsError() throws Exception {
        // Arrange
        Set<String> hostnames = Set.of("master0.example.com", "worker0.example.com");
        StackDtoDelegate stack = aStack();
        OrchestratorStateParams params = mock(OrchestratorStateParams.class);
        GatewayConfig mockGatewayConfig = GatewayConfig.builder().withHostname("master0.example.com").build();
        KerberosConfig mockKerberosConfig = mock(KerberosConfig.class);
        when(kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName())).thenReturn(Optional.of(mockKerberosConfig));

        // Act
        List<String> errors = underTest.cleanUpAd(hostnames, stack);

        // Assert
        assertEquals("AD Cleanup failed, LDAP config not found for stack: " + stack.getName(), errors.getFirst());
    }

    @Test
    void testCleanUpAdNoHostnames() throws CloudbreakOrchestratorFailedException {
        // Arrange
        Set<String> hostnames = Set.of();
        StackDto stack = mock(StackDto.class);

        // Act
        List<String> errors = underTest.cleanUpAd(hostnames, stack);

        // Assert
        assertTrue(errors.isEmpty());
        verify(hostOrchestrator, never()).runOrchestratorState(any());
    }

    @Test
    void testCleanUpAdOrchestratorStateThrowsException() throws CloudbreakOrchestratorFailedException {
        // Arrange
        String host1 = "master0.example.com";
        String host2 = "worker0.example.com";
        Set<String> hostnames = Set.of(host1, host2);
        StackDtoDelegate stack = aStack();
        GatewayConfig mockGatewayConfig = GatewayConfig.builder().withHostname("master0.example.com").build();
        KerberosConfig mockKerberosConfig = mock(KerberosConfig.class);
        LdapView mockLdapView = mock(LdapView.class);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(mockGatewayConfig);
        when(kerberosConfigService.get(stack.getEnvironmentCrn(), stack.getName())).thenReturn(Optional.of(mockKerberosConfig));
        when(ldapConfigService.get(stack.getEnvironmentCrn(), stack.getName())).thenReturn(Optional.of(mockLdapView));

        doThrow(new CloudbreakOrchestratorFailedException("Error"))
                .when(hostOrchestrator).runOrchestratorState(any(OrchestratorStateParams.class));

        // Act & Assert
        assertThrows(CloudbreakOrchestratorFailedException.class, () -> underTest.cleanUpAd(hostnames, stack));
    }

    private StackDtoDelegate aStack() {
        Stack stack = new Stack();
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        stack.setName(STACK_NAME);
        stack.setCloudPlatform(CLOUD_PLATFORM);
        stack.setResourceCrn(CrnTestUtil.getDatalakeCrnBuilder()
                .setAccountId("accountId")
                .setResource("resource")
                .build().toString());
        return stack;
    }

}