package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class ClusterServiceRunnerTest {

    @Mock
    private Stack stack;

    @Mock
    private Cluster cluster;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterHostServiceRunner hostRunner;

    @InjectMocks
    private ClusterServiceRunner underTest;

    @Test
    public void testRedeployGatewayCertificateWhenClusterCouldNotBeFoundByStackId() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenThrow(new NotFoundException("Cluster could not be found"));

        Assertions.assertThrows(NotFoundException.class, () -> underTest.redeployGatewayCertificate(0L));
    }

    @Test
    public void testRedeployGatewayCertificateWhenRedeployOnHostRunnerThrowRuntimeException() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenReturn(Optional.of(cluster));
        doThrow(new RuntimeException("Stg. failed")).when(hostRunner).redeployGatewayCertificate(any(), any());

        Assertions.assertThrows(RuntimeException.class, () -> underTest.redeployGatewayCertificate(0L));
    }

    @Test
    public void testRedeployGatewayCertificateHappyFlow() {
        when(stackService.getByIdWithListsInTransaction(anyLong())).thenReturn(stack);
        when(clusterService.retrieveClusterByStackIdWithoutAuth(anyLong())).thenReturn(Optional.of(cluster));

        underTest.redeployGatewayCertificate(0L);

        verify(hostRunner, times(1)).redeployGatewayCertificate(any(), any());
    }

}
