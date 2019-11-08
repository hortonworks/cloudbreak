package com.sequenceiq.cloudbreak.service.cluster;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.HostOrchestratorResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.template.kerberos.KerberosDetailService;
import com.sequenceiq.cloudbreak.util.StackUtil;

public class ClusterKerberosServiceTest {
    @Mock
    private HostOrchestratorResolver hostOrchestratorResolver;

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

    private Stack stack;

    private Cluster cluster;

    private KerberosConfig kerberosConfig;

    @Before
    public void init() throws CloudbreakException {
        MockitoAnnotations.initMocks(this);
        stack = new Stack();
        Orchestrator orchestrator = new Orchestrator();
        orchestrator.setType("HOST");
        stack.setOrchestrator(orchestrator);
        cluster = spy(new Cluster());
        stack.setCluster(cluster);
        kerberosConfig = KerberosConfig.KerberosConfigBuilder.aKerberosConfig().build();
        when(hostOrchestratorResolver.get(anyString())).thenReturn(hostOrchestrator);
        when(gatewayConfigService.getPrimaryGatewayConfig(stack)).thenReturn(new GatewayConfig("a", "a", "a", 1, "a", false));
    }

    @Test
    public void testAdLeave() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.isAdJoinable(any())).thenReturn(Boolean.TRUE);

        underTest.leaveDomains(stack, false);

        verify(hostOrchestrator, times(1))
                .leaveDomain(any(GatewayConfig.class), any(), eq("ad_member"), eq("ad_leave"), any(ExitCriteriaModel.class), anyBoolean());
    }

    @Test
    public void testIpaLeave() throws CloudbreakException, CloudbreakOrchestratorFailedException {
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.isIpaJoinable(any())).thenReturn(Boolean.TRUE);

        underTest.leaveDomains(stack, false);

        verify(hostOrchestrator, times(1))
                .leaveDomain(any(GatewayConfig.class), any(), eq("ipa_member"), eq("ipa_leave"), any(ExitCriteriaModel.class), anyBoolean());
    }

    @Test(expected = CloudbreakException.class)
    public void testExceptionMapped() throws CloudbreakOrchestratorFailedException, CloudbreakException {
        when(kerberosConfigService.get(anyString(), anyString())).thenReturn(Optional.of(kerberosConfig));
        when(kerberosDetailService.isAdJoinable(any())).thenReturn(Boolean.TRUE);

        doThrow(new CloudbreakOrchestratorFailedException("error")).when(hostOrchestrator)
                .leaveDomain(any(GatewayConfig.class), any(), eq("ad_member"), eq("ad_leave"), any(ExitCriteriaModel.class), anyBoolean());

        underTest.leaveDomains(stack, false);
    }

}