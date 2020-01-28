package com.sequenceiq.freeipa.service.freeipa;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;

@ExtendWith(MockitoExtension.class)
class FreeIpaClientFactoryTest {

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private TlsSecurityService tlsSecurityService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @InjectMocks
    private FreeIpaClientFactory underTest;

    @Test
    void getFreeIpaClientForStackShouldThrowExceptionWhenStackStatusIsUnreachable() {
        Stack stack = new Stack();
        Status unreachableState = Status.FREEIPA_UNREACHABLE_STATUSES.stream().findAny().get();
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is unreachable.", DetailedStackStatus.UNREACHABLE);
        stack.setStackStatus(stackStatus);

        Assertions.assertThrows(FreeIpaClientException.class, () -> underTest.getFreeIpaClientForStack(stack));
    }

    @Test
    void getFreeIpaClientForStackShouldReturnClientWhenStackStatusIsValid() throws FreeIpaClientException {
        Stack stack = new Stack();
        Status unreachableState = Status.AVAILABLE;
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is reachable.", DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(stackStatus);
        when(clusterProxyService.isCreateConfigForClusterProxy(stack)).thenThrow(new RuntimeException("Expected exception"));

        FreeIpaClientException exception = Assertions.assertThrows(FreeIpaClientException.class, () -> underTest.getFreeIpaClientForStack(stack));

        verify(clusterProxyService, times(1)).isCreateConfigForClusterProxy(stack);
        Assertions.assertEquals(RuntimeException.class, exception.getCause().getClass());
    }

    @Test
    void getFreeIpaClientForStackWithPingShouldThrowExceptionWhenStackStatusIsUnreachable() {
        Stack stack = new Stack();
        Status unreachableState = Status.AVAILABLE;
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is reachable.", DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(stackStatus);
        when(clusterProxyService.isCreateConfigForClusterProxy(stack)).thenThrow(new RuntimeException("Expected exception"));

        FreeIpaClientException exception = Assertions.assertThrows(FreeIpaClientException.class, () -> underTest.getFreeIpaClientForStackWithPing(stack));

        verify(clusterProxyService, times(1)).isCreateConfigForClusterProxy(stack);
        Assertions.assertEquals(RuntimeException.class, exception.getCause().getClass());
    }

    @Test
    void getFreeIpaClientForStackWithPingShouldReturnClientWhenStackStatusIsValid() {
        Stack stack = new Stack();
        Status unreachableState = Status.FREEIPA_UNREACHABLE_STATUSES.stream().findAny().get();
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is unreachable.", DetailedStackStatus.UNREACHABLE);
        stack.setStackStatus(stackStatus);

        Assertions.assertThrows(FreeIpaClientException.class, () -> underTest.getFreeIpaClientForStackWithPing(stack));
    }
}