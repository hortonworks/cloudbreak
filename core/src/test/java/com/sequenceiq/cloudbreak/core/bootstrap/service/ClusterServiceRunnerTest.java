package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class ClusterServiceRunnerTest {

    @Mock
    private StackDto stackDto;

    @Mock
    private Cluster cluster;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterHostServiceRunner hostRunner;

    @InjectMocks
    private ClusterServiceRunner underTest;

    @Test
    void testRedeployGatewayCertificateWhenRedeployOnHostRunnerThrowRuntimeException() {
        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        doThrow(new RuntimeException("Stg. failed")).when(hostRunner).redeployGatewayCertificate(any());

        Assertions.assertThrows(RuntimeException.class, () -> underTest.redeployGatewayCertificate(0L));
    }

    @Test
    void testRedeployGatewayCertificateHappyFlow() {
        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);

        underTest.redeployGatewayCertificate(0L);

        verify(hostRunner, times(1)).redeployGatewayCertificate(any());
    }

    @Test
    void testRedeployGatewayPillar() {
        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        underTest.redeployGatewayPillar(1L);
        verify(hostRunner).redeployGatewayPillarOnly(stackDto);
    }

    @Test
    void testRedeployStates() {
        when(stackDtoService.getById(anyLong())).thenReturn(stackDto);
        underTest.redeployStates(1L);
        verify(hostRunner).redeployStates(stackDto);
    }
}
