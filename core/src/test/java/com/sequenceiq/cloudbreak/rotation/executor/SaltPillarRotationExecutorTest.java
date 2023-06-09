package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationProgressService;
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

    @Mock
    private ExitCriteriaProvider exitCriteriaProvider;

    @Mock
    private SecretRotationProgressService secretRotationProgressService;

    @InjectMocks
    private SaltPillarRotationExecutor underTest;

    @BeforeEach
    void setup() throws IllegalAccessException {
        lenient().when(exitCriteriaProvider.get(any())).thenReturn(ClusterDeletionBasedExitCriteriaModel.nonCancellableModel());
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.of(secretRotationProgressService), true);
        lenient().when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void validationShouldSucceed() throws CloudbreakOrchestratorFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        StackDto stackDto = mockStackDto();
        doNothing().when(hostOrchestrator).ping(any(), any());

        underTest.executePreValidation(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()));

        verify(hostOrchestrator).ping(any(), any());
    }

    @Test
    void validationShouldFailIfSaltPingFails() throws CloudbreakOrchestratorFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        StackDto stackDto = mockStackDto();
        doThrow(new CloudbreakOrchestratorFailedException("pingpong")).when(hostOrchestrator).ping(any(), any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executePreValidation(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator())));

        verify(hostOrchestrator).ping(any(), any());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfStackNotFound() {
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenThrow(NotFoundException.notFound("Stack", RESOURCE_CRN).get());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null));
        Assertions.assertEquals("Execution of rotation failed at SALT_PILLAR step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfSaltFails() throws CloudbreakOrchestratorFailedException {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        StackDto stackDto = mockStackDto();
        doThrow(new RuntimeException("error")).when(hostOrchestrator)
                .saveCustomPillars(any(), any(), any());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null));
        Assertions.assertEquals("Execution of rotation failed at SALT_PILLAR step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
        verify(hostOrchestrator, times(1)).saveCustomPillars(any(), any(), any());
        verify(saltStateParamsService, times(1)).createStateParams(eq(stackDto), isNull(), eq(true), anyInt(), anyInt());
    }

    @Test
    void rotateShouldSucceed() throws Exception {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        StackDto stackDto = mockStackDto();
        underTest.executeRotate(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null);
        verify(hostOrchestrator, times(1)).saveCustomPillars(any(), any(), any());
        verify(saltStateParamsService, times(1)).createStateParams(eq(stackDto), isNull(), eq(true), anyInt(), anyInt());
    }

    @Test
    void rollbackShouldSucceed() throws Exception {
        when(saltStateParamsService.createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt())).thenReturn(new OrchestratorStateParams());
        StackDto stackDto = mockStackDto();
        underTest.executeRollback(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null);
        verify(hostOrchestrator, times(1)).saveCustomPillars(any(), any(), any());
        verify(saltStateParamsService, times(1)).createStateParams(eq(stackDto), isNull(), eq(true), anyInt(), anyInt());
    }

    @Test
    void finalizeShouldDoNothing() throws CloudbreakOrchestratorFailedException {
        underTest.executeFinalize(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null);
        verify(hostOrchestrator, never()).saveCustomPillars(any(), any(), any());
        verify(saltStateParamsService, never()).createStateParams(any(), any(), anyBoolean(), anyInt(), anyInt());
    }

    private StackDto mockStackDto() {
        StackDto stackDto = mock(StackDto.class);
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        lenient().when(stackDto.getCluster()).thenReturn(cluster);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        return stackDto;
    }

    private Function getSaltPillarGenerator() {
        Function saltPillarGenerator = mock(Function.class);
        lenient().when(saltPillarGenerator.apply(any())).thenReturn(new HashMap<>());
        return saltPillarGenerator;
    }
}