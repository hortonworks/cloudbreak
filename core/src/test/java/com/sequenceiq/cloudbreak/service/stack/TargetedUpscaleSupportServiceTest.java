package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.InternalServerErrorException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.stack.DnsResolverType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.StackView;

@ExtendWith(MockitoExtension.class)
public class TargetedUpscaleSupportServiceTest {

    private static final String ACCOUNT_ID = "1234";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:eu-1:" + ACCOUNT_ID + ":user:91011";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @InjectMocks
    private TargetedUpscaleSupportService underTest;

    @Test
    public void testIfEntitlementsDisabled() {
        when(entitlementService.targetedUpscaleSupported(any())).thenReturn(Boolean.TRUE);
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(Boolean.FALSE);
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.UNKNOWN)));

        when(entitlementService.targetedUpscaleSupported(any())).thenReturn(Boolean.FALSE);
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.UNKNOWN)));

        verify(entitlementService, times(2)).targetedUpscaleSupported(eq(ACCOUNT_ID));
        verify(entitlementService, times(1)).isUnboundEliminationSupported(eq(ACCOUNT_ID));
    }

    @Test
    public void testIfFreeipaDnsResolver() {
        when(entitlementService.targetedUpscaleSupported(any())).thenReturn(Boolean.TRUE);
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(Boolean.TRUE);
        assertTrue(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.FREEIPA_FOR_ENV)));

        verify(entitlementService, times(1)).targetedUpscaleSupported(eq(ACCOUNT_ID));
        verify(entitlementService, times(1)).isUnboundEliminationSupported(eq(ACCOUNT_ID));
    }

    @Test
    public void testIfUnboundDnsResolver() {
        when(entitlementService.targetedUpscaleSupported(any())).thenReturn(Boolean.TRUE);
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(Boolean.TRUE);
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.LOCAL_UNBOUND)));

        verify(entitlementService, times(1)).targetedUpscaleSupported(eq(ACCOUNT_ID));
        verify(entitlementService, times(1)).isUnboundEliminationSupported(eq(ACCOUNT_ID));
    }

    @Test
    public void testIfThereIsAnyError() {
        when(entitlementService.targetedUpscaleSupported(any())).thenThrow(new InternalServerErrorException("error"));
        assertFalse(underTest.targetedUpscaleOperationSupported(getStack(DnsResolverType.UNKNOWN)));

        verify(entitlementService, times(1)).targetedUpscaleSupported(eq(ACCOUNT_ID));
    }

    @ParameterizedTest(name = "unboundConfigPresentOnAnyNodes {0}, " +
            "unboundEliminationSupported {1}, domainDnsResolverType {2}.")
    @MethodSource("testUpdatingStackDnsResolverData")
    public void testUpdatingStackDnsResolver(Boolean unboundConfigPresentOnAnyNodes, Boolean unboundEliminationSupported, DnsResolverType result) {
        lenient().when(gatewayConfigService.getPrimaryGatewayConfig(any())).thenReturn(
                GatewayConfig.builder()
                        .withConnectionAddress("host1")
                        .withPublicAddress("1.1.1.1")
                        .withPrivateAddress("1.1.1.1")
                        .withGatewayPort(22)
                        .withInstanceId("i-1839")
                        .withKnoxGatewayEnabled(false)
                        .build()
        );
        lenient().when(stackUtil.collectReachableNodes(any())).thenReturn(Set.of());
        lenient().when(hostOrchestrator.unboundClusterConfigPresentOnAnyNodes(any(), any())).thenReturn(unboundConfigPresentOnAnyNodes);
        when(entitlementService.isUnboundEliminationSupported(any())).thenReturn(unboundEliminationSupported);

        assertEquals(result, underTest.getActualDnsResolverType(getStackDto()));

        verify(entitlementService, times(1)).isUnboundEliminationSupported(eq(ACCOUNT_ID));
    }

    private static Stream<Arguments> testUpdatingStackDnsResolverData() {
        return Stream.of(
                Arguments.of(Boolean.TRUE, Boolean.TRUE, DnsResolverType.LOCAL_UNBOUND),
                Arguments.of(Boolean.TRUE, Boolean.FALSE, DnsResolverType.LOCAL_UNBOUND),
                Arguments.of(Boolean.FALSE, Boolean.TRUE, DnsResolverType.FREEIPA_FOR_ENV),
                Arguments.of(Boolean.FALSE, Boolean.FALSE, DnsResolverType.LOCAL_UNBOUND)
        );
    }

    private StackView getStack(DnsResolverType dnsResolverType) {
        Stack stack = new Stack();
        stack.setResourceCrn(DATAHUB_CRN);
        stack.setDomainDnsResolver(dnsResolverType);
        return stack;
    }

    private StackDto getStackDto() {
        Stack stack = new Stack();
        stack.setResourceCrn(DATAHUB_CRN);
        return new StackDto(stack, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
