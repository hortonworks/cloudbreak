package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class TargetedUpscaleSupportServiceTest {

    private static final String DATAHUB_CRN = "crn:cdp:datahub:eu-1:1234:user:91011";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private StackUtil stackUtil;

    @InjectMocks
    private TargetedUpscaleSupportService underTest;

    @Test
    public void testIfEntitlementDisabled() {
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(Boolean.FALSE);
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack()));
    }

    @Test
    public void testIfUnboundConfigPresent() {
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(Boolean.TRUE);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(new GatewayConfig(null, null, null, null, null, null));
        when(stackUtil.collectReachableNodes(any())).thenReturn(Set.of());
        when(hostOrchestrator.unboundClusterConfigPresentOnAnyNodes(any(), any())).thenReturn(Boolean.TRUE);
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack()));
    }

    @Test
    public void testIfUnboundConfigNotPresent() {
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(Boolean.TRUE);
        when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(new GatewayConfig(null, null, null, null, null, null));
        when(stackUtil.collectReachableNodes(any())).thenReturn(Set.of());
        when(hostOrchestrator.unboundClusterConfigPresentOnAnyNodes(any(), any())).thenReturn(Boolean.FALSE);
        assertTrue(underTest.targetedUpscaleOperationSupported(getStack()));
    }

    @Test
    public void testIfThereIsAnyError() {
        when(entitlementService.isUnboundEliminationSupported(any())).thenThrow(new InternalServerErrorException("error"));
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack()));
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setResourceCrn(DATAHUB_CRN);
        return stack;
    }
}
