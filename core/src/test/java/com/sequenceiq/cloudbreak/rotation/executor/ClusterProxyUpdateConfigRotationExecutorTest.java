package com.sequenceiq.cloudbreak.rotation.executor;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyUpdateConfigRotationContext.ClusterProxyUpdateConfigRotationContextBuilder;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class ClusterProxyUpdateConfigRotationExecutorTest {

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stackDto;

    @InjectMocks
    private ClusterProxyUpdateConfigRotationExecutor underTest;

    @Test
    public void testRotation() throws Exception {
        when(stackDtoService.getByCrn("resource")).thenReturn(stackDto);
        when(stackDto.getId()).thenReturn(1L);

        underTest.rotate(new ClusterProxyUpdateConfigRotationContextBuilder().withResourceCrn("resource")
                .withKnoxSecretPath("secretPath")
                .build());

        verify(clusterProxyService).updateClusterConfigWithKnoxSecretLocation(1L, "secretPath");
    }

    @Test
    public void testRollback() throws Exception {
        when(stackDtoService.getByCrn("resource")).thenReturn(stackDto);
        when(stackDto.getId()).thenReturn(1L);

        underTest.rollback(new ClusterProxyUpdateConfigRotationContextBuilder().withResourceCrn("resource")
                .withKnoxSecretPath("secretPath")
                .build());

        verify(clusterProxyService).updateClusterConfigWithKnoxSecretLocation(1L, "secretPath");
    }

}