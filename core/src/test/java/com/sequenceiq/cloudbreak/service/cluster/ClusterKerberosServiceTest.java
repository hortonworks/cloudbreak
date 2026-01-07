package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
public class ClusterKerberosServiceTest {
    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private KerberosDetailService kerberosDetailService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @InjectMocks
    private ClusterKerberosService underTest;

    @Mock
    private Node node;

    @Mock
    private StackDto stack;

    @Mock
    private StackView stackView;

    private KerberosConfig kerberosConfig;

    @BeforeEach
    public void init() throws CloudbreakException {
        when(stackView.getName()).thenReturn("");
        when(stackView.getEnvironmentCrn()).thenReturn("");
        when(stack.getStack()).thenReturn(stackView);
        kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().build();
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(GatewayConfig.builder()
                .withConnectionAddress("host1")
                .withPublicAddress("1.1.1.1")
                .withPrivateAddress("1.1.1.1")
                .withGatewayPort(22)
                .withInstanceId("i-1839")
                .withKnoxGatewayEnabled(false)
                .build()
        );
    }

    @Test
    public void testAdLeave() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.isAdJoinable(any())).thenReturn(Boolean.TRUE);
        when(stackUtil.collectReachableNodes(any())).thenReturn(Set.of(node));

        underTest.leaveDomains(stack);

        verify(stackUtil).collectReachableNodes(stack);
        verify(hostOrchestrator, times(1))
                .leaveDomain(any(GatewayConfig.class), eq(Set.of(node)), eq("ad_member"), eq("ad_leave"), any(ExitCriteriaModel.class));
    }

    @Test
    public void testIpaLeave() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.isIpaJoinable(any())).thenReturn(Boolean.TRUE);

        underTest.leaveDomains(stack);

        verify(hostOrchestrator, times(1))
                .leaveDomain(any(GatewayConfig.class), any(), eq("ipa_member"), eq("ipa_leave"), any(ExitCriteriaModel.class));
    }

    @Test
    public void testExceptionMapped() throws CloudbreakOrchestratorFailedException {
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.isAdJoinable(any())).thenReturn(Boolean.TRUE);

        doThrow(new CloudbreakOrchestratorFailedException("error")).when(hostOrchestrator)
                .leaveDomain(any(GatewayConfig.class), any(), eq("ad_member"), eq("ad_leave"), any(ExitCriteriaModel.class));

        assertThrows(CloudbreakException.class, () -> underTest.leaveDomains(stack));
    }

}