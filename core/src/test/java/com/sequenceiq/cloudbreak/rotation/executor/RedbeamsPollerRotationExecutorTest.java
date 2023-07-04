package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.redbeams.rotation.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.externaldatabase.ExternalDatabaseService;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.RotationFlowExecutionType;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.PollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.SecretRotationProgressService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class RedbeamsPollerRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String DATABASE_SERVER_CRN = "databaseServerCrn";

    @Mock
    private ExternalDatabaseService externalDatabaseService;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private SecretRotationProgressService secretRotationProgressService;

    @InjectMocks
    private RedbeamsPollerRotationExecutor underTest;

    @BeforeEach
    public void mockProgressService() throws IllegalAccessException {
        FieldUtils.writeField(underTest, "secretRotationProgressService", Optional.of(secretRotationProgressService), true);
        lenient().when(secretRotationProgressService.latestStep(any(), any(), any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfStackNotFound() {
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenThrow(NotFoundException.notFound("Stack", RESOURCE_CRN).get());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Execution of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfDatabaseServerCrnNotFound() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(new Cluster());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Execution of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfExternalCallFails() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        doThrow(new RuntimeException("error")).when(externalDatabaseService)
                .rotateDatabaseSecret(eq(RESOURCE_CRN), eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.ROTATE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Execution of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void rotateShouldSucceed() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(stackDtoService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(externalDatabaseService, times(1)).rotateDatabaseSecret(eq(DATABASE_SERVER_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.ROTATE));
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfStackNotFound() {
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenThrow(NotFoundException.notFound("Stack", RESOURCE_CRN).get());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Rollback of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfDatabaseServerCrnNotFound() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(new Cluster());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Rollback of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfExternalCallFails() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        doThrow(new RuntimeException("error")).when(externalDatabaseService)
                .rotateDatabaseSecret(eq(RESOURCE_CRN), eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.ROLLBACK));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Rollback of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldSucceed() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(stackDtoService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(externalDatabaseService, times(1)).rotateDatabaseSecret(eq(DATABASE_SERVER_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.ROLLBACK));
    }

    @Test
    void finalizeShouldThrowSecretRotationExceptionIfStackNotFound() {
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenThrow(NotFoundException.notFound("Stack", RESOURCE_CRN).get());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Finalization of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void finalizeShouldThrowSecretRotationExceptionIfDatabaseServerCrnNotFound() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(new Cluster());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Finalization of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void finalizeShouldThrowSecretRotationExceptionIfExternalCallFails() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        doThrow(new RuntimeException("error")).when(externalDatabaseService)
                .rotateDatabaseSecret(eq(RESOURCE_CRN), eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.FINALIZE));
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null));
        Assertions.assertEquals("Finalization of rotation failed at REDBEAMS_ROTATE_POLLING step for resourceCrn regarding secret null.",
                secretRotationException.getMessage());
    }

    @Test
    void finalizeShouldSucceed() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(stackDtoService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(externalDatabaseService, times(1)).rotateDatabaseSecret(eq(DATABASE_SERVER_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.FINALIZE));
    }

}