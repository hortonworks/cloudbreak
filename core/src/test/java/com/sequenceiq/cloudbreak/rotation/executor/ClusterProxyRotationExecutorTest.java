package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.clusterproxy.ConfigRegistrationResponse;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.service.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.ClusterProxyRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.progress.SecretRotationProgressService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
public class ClusterProxyRotationExecutorTest {

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterProxyService clusterProxyService;

    @Mock
    private SecretRotationProgressService secretRotationProgressService;

    @InjectMocks
    private ClusterProxyRotationExecutor underTest;

    @BeforeEach
    public void mockProgressService() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.of(secretRotationProgressService), true);
        lenient().when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    public void testRotation() throws IllegalAccessException {
        StackDto stackDto = getStackDto();
        when(stackDtoService.getByCrn(any())).thenReturn(stackDto);
        when(clusterProxyService.reRegisterCluster(anyLong())).thenReturn(Optional.of(new ConfigRegistrationResponse()));

        underTest.executeRotate(ClusterProxyRotationContext.builder().withResourceCrn("resource").build(), null);

        verify(clusterProxyService).reRegisterCluster(eq(1L));
    }

    @Test
    public void testRotationFailure() throws IllegalAccessException {
        StackDto stackDto = getStackDto();
        when(stackDtoService.getByCrn(any())).thenReturn(stackDto);
        when(clusterProxyService.reRegisterCluster(anyLong())).thenThrow(new CloudbreakServiceException("something"));

        assertThrows(SecretRotationException.class, () ->
                underTest.executeRotate(ClusterProxyRotationContext.builder().withResourceCrn("resource").build(), null));

        verify(clusterProxyService).reRegisterCluster(eq(1L));
    }

    @Test
    public void testRollback() throws IllegalAccessException {
        StackDto stackDto = getStackDto();
        when(stackDtoService.getByCrn(any())).thenReturn(stackDto);
        when(clusterProxyService.reRegisterCluster(anyLong())).thenReturn(Optional.of(new ConfigRegistrationResponse()));

        underTest.executeRollback(ClusterProxyRotationContext.builder().withResourceCrn("resource").build(), null);

        verify(clusterProxyService).reRegisterCluster(eq(1L));
    }

    private static StackDto getStackDto() throws IllegalAccessException {
        StackDto stackDto = new StackDto();
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setResourceCrn("resourceCrn");
        FieldUtils.writeDeclaredField(stackDto, "stack", stack, true);
        return stackDto;
    }
}
