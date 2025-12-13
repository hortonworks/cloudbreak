package com.sequenceiq.freeipa.service.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.client.HttpClientConfig;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.client.FreeIpaClientBuildException;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.InvalidFreeIpaStateException;
import com.sequenceiq.freeipa.entity.FreeIpa;
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

    private static final Set<InstanceMetaData> METADATAS = new HashSet<>();

    private static final String FREEIPP_FQDN = "localhost";

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

    @BeforeAll
    static void setup() {
        InstanceMetaData metaData = new InstanceMetaData();
        METADATAS.add(metaData);
        metaData.setPrivateIp("127.0.0.1");
        metaData.setDiscoveryFQDN(FREEIPP_FQDN);
        metaData.setInstanceStatus(InstanceStatus.CREATED);
    }

    @Test
    void getFreeIpaClientForStackShouldThrowExceptionWhenStackStatusIsUnreachable() {
        Stack stack = createStack();
        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
        Status unreachableState = Status.FREEIPA_UNREACHABLE_STATUSES.stream().findAny().get();
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is unreachable.", DetailedStackStatus.UNREACHABLE);
        stack.setStackStatus(stackStatus);

        assertThrows(InvalidFreeIpaStateException.class, () -> underTest.getFreeIpaClientForStack(stack));
    }

    @Test
    void getFreeIpaClientForStackShouldReturnClientWhenStackStatusIsValid() throws FreeIpaClientException {
        Stack stack = createStack();
        stack.setGatewayport(80);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword(new Secret("", ""));
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
        when(tlsSecurityService.buildTLSClientConfig(any(), any(), any())).thenReturn(new HttpClientConfig(FREEIPP_FQDN));
        Status unreachableState = Status.AVAILABLE;
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is reachable.", DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(stackStatus);
        when(clusterProxyService.isCreateConfigForClusterProxy(stack)).thenReturn(false);

        FreeIpaClientException exception = assertThrows(FreeIpaClientException.class, () -> underTest.getFreeIpaClientForStack(stack));

        verify(clusterProxyService, times(1)).isCreateConfigForClusterProxy(stack);
        verify(tlsSecurityService, times(1)).buildTLSClientConfig(any(), any(), any());
        assertEquals(FreeIpaClientBuildException.class, exception.getCause().getClass());
    }

    @Test
    void getFreeIpaClientForStackForLegacyHealthCheckShouldReturnClientWhenStackStatusIsUnreachable() {
        Stack stack = createStack();
        stack.setGatewayport(80);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword(new Secret("", ""));
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
        when(tlsSecurityService.buildTLSClientConfig(any(), any(), any())).thenReturn(new HttpClientConfig(FREEIPP_FQDN));
        Status unreachableState = Status.FREEIPA_UNREACHABLE_STATUSES.stream().findAny().get();
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is unreachable.", DetailedStackStatus.UNREACHABLE);
        stack.setStackStatus(stackStatus);

        FreeIpaClientException exception = assertThrows(FreeIpaClientException.class, () ->
                underTest.getFreeIpaClientForStackForLegacyHealthCheck(stack, FREEIPP_FQDN));

        verify(clusterProxyService, times(1)).isCreateConfigForClusterProxy(stack);
        verify(tlsSecurityService, times(1)).buildTLSClientConfig(any(), any(), any());
        assertEquals(FreeIpaClientBuildException.class, exception.getCause().getClass());
    }

    @Test
    void getFreeIpaClientForStackForLegacyHealthCheckShouldReturnClientWhenStackStatusIsValid() {
        Stack stack = createStack();
        stack.setGatewayport(80);
        FreeIpa freeIpa = new FreeIpa();
        freeIpa.setAdminPassword(new Secret("", ""));
        when(freeIpaService.findByStack(stack)).thenReturn(freeIpa);
        when(stackService.getByIdWithListsInTransaction(stack.getId())).thenReturn(stack);
        when(tlsSecurityService.buildTLSClientConfig(any(), any(), any())).thenReturn(new HttpClientConfig(FREEIPP_FQDN));
        Status unreachableState = Status.AVAILABLE;
        StackStatus stackStatus = new StackStatus(stack, unreachableState, "The FreeIPA instance is reachable.", DetailedStackStatus.AVAILABLE);
        stack.setStackStatus(stackStatus);

        FreeIpaClientException exception = assertThrows(FreeIpaClientException.class, () ->
                underTest.getFreeIpaClientForStackForLegacyHealthCheck(stack, FREEIPP_FQDN));

        verify(clusterProxyService, times(1)).isCreateConfigForClusterProxy(stack);
        verify(tlsSecurityService, times(1)).buildTLSClientConfig(any(), any(), any());
        assertEquals(FreeIpaClientBuildException.class, exception.getCause().getClass());
    }

    private Stack createStack() {
        Stack stack = new Stack();
        Set<InstanceGroup> instanceGroups = new HashSet<>();
        stack.setInstanceGroups(instanceGroups);
        InstanceGroup group = new InstanceGroup();
        instanceGroups.add(group);
        group.setInstanceMetaData(METADATAS);
        return stack;
    }
}
