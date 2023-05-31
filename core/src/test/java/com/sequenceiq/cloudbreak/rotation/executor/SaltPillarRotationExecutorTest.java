package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class SaltPillarRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final Long CLUSTER_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private SaltStateParamsService saltStateParamsService;

    @InjectMocks
    private SaltPillarRotationExecutor underTest;

    @Test
    void rotateShouldThrowSecretRotationExceptionIfStackNotFound() {
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenThrow(NotFoundException.notFound("Stack", RESOURCE_CRN).get());
        Function saltPillarGenerator = mock(Function.class);
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.rotate(new SaltPillarRotationContext(RESOURCE_CRN, saltPillarGenerator)));
        Assertions.assertEquals("Salt pillar rotation failed for resourceCrn", secretRotationException.getMessage());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfSaltFails() throws CloudbreakOrchestratorFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        StackDto stackDto = mock(StackDto.class);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Function saltPillarGenerator = mock(Function.class);
        doThrow(new RuntimeException("error")).when(hostOrchestrator)
                .saveCustomPillars(any(), any(), any());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.rotate(new SaltPillarRotationContext(RESOURCE_CRN, saltPillarGenerator)));
        Assertions.assertEquals("Salt pillar rotation failed for resourceCrn", secretRotationException.getMessage());
        verify(hostOrchestrator, times(1)).saveCustomPillars(any(), any(), any());
        verify(saltStateParamsService, times(1)).createStateParams(eq(stackDto), isNull(), eq(true), anyInt(), anyInt());
    }

    @Test
    void rotateShouldSucceed() throws CloudbreakOrchestratorFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        StackDto stackDto = mock(StackDto.class);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Function saltPillarGenerator = mock(Function.class);
        underTest.rotate(new SaltPillarRotationContext(RESOURCE_CRN, saltPillarGenerator));
        verify(hostOrchestrator, times(1)).saveCustomPillars(any(), any(), any());
        verify(saltStateParamsService, times(1)).createStateParams(eq(stackDto), isNull(), eq(true), anyInt(), anyInt());
    }

    @Test
    void rollbackShouldSucceed() throws CloudbreakOrchestratorFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        StackDto stackDto = mock(StackDto.class);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Function saltPillarGenerator = mock(Function.class);
        underTest.rollback(new SaltPillarRotationContext(RESOURCE_CRN, saltPillarGenerator));
        verify(hostOrchestrator, times(1)).saveCustomPillars(any(), any(), any());
        verify(saltStateParamsService, times(1)).createStateParams(eq(stackDto), isNull(), eq(true), anyInt(), anyInt());
    }

    @Test
    void finalizeShouldDoNothing() throws CloudbreakOrchestratorFailedException {
        Function saltPillarGenerator = mock(Function.class);
        underTest.finalize(new SaltPillarRotationContext(RESOURCE_CRN, saltPillarGenerator));
        verify(hostOrchestrator, never()).saveCustomPillars(any(), any(), any());
        verify(saltStateParamsService, never()).createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt());
    }
}