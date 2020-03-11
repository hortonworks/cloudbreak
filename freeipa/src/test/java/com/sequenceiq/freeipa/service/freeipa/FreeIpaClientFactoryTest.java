package com.sequenceiq.freeipa.service.freeipa;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.service.GatewayConfigService;
import com.sequenceiq.freeipa.service.TlsSecurityService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaClientFactoryTest {

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private StackService stackService;

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
        Stack stack = createStack();
        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
        Status unreachableState = Status.FREEIPA_UNREACHABLE_STATUSES.stream().findAny().get();
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is unreachable.", DetailedStackStatus.UNREACHABLE);
        stack.setStackStatus(stackStatus);

        Assertions.assertThrows(FreeIpaClientException.class, () -> underTest.getFreeIpaClientForStack(stack));
    }

    @Test
    void getFreeIpaClientForStackShouldReturnClientWhenStackStatusIsValid() throws FreeIpaClientException {
        Stack stack = createStack();
        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
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
        Stack stack = createStack();
        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
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
        Stack stack = createStack();
        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
        Status unreachableState = Status.FREEIPA_UNREACHABLE_STATUSES.stream().findAny().get();
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is unreachable.", DetailedStackStatus.UNREACHABLE);
        stack.setStackStatus(stackStatus);

        Assertions.assertThrows(FreeIpaClientException.class, () -> underTest.getFreeIpaClientForStackWithPing(stack));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        stack.setInstanceGroups(instanceGroups);
        InstanceGroup group = new InstanceGroup();
        instanceGroups.add(group);
        Set<InstanceMetaData> metaDatas = new HashSet<>();
        group.setInstanceMetaData(metaDatas);
        InstanceMetaData metaData = new InstanceMetaData();
        metaDatas.add(metaData);
        metaData.setPrivateIp("1.1.1.1");
        return stack;
    }
}