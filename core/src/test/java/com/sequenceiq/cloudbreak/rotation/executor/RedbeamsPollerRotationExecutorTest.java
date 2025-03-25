package com.sequenceiq.cloudbreak.rotation.executor;

import static com.sequenceiq.redbeams.rotation.RedbeamsSecretType.REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.sequenceiq.cloudbreak.rotation.RotationMetadataTestUtil;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.poller.PollerRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
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
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private RedbeamsPollerRotationExecutor underTest;

    @Test
    void rotateShouldThrowSecretRotationExceptionIfStackNotFound() {
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenThrow(NotFoundException.notFound("Stack", RESOURCE_CRN).get());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForRotation(RESOURCE_CRN, null)));
        assertEquals("Execution of rotation failed at REDBEAMS_ROTATE_POLLING step regarding secret null, " +
                        "reason: Stack 'resourceCrn' not found.", secretRotationException.getMessage());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfDatabaseServerCrnNotFound() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(new Cluster());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForRotation(RESOURCE_CRN, null)));
        assertEquals("Execution of rotation failed at REDBEAMS_ROTATE_POLLING step regarding secret null, " +
                        "reason: No database server found for cluster: resourceCrn", secretRotationException.getMessage());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfExternalCallFails() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        doThrow(new RuntimeException("error")).when(externalDatabaseService)
                .rotateDatabaseSecret(eq(DATABASE_SERVER_CRN), eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.ROTATE), any());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForRotation(RESOURCE_CRN, null)));
        assertEquals("Execution of rotation failed at REDBEAMS_ROTATE_POLLING step regarding secret null, reason: error",
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
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.ROTATE), any());
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfStackNotFound() {
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenThrow(NotFoundException.notFound("Stack", RESOURCE_CRN).get());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForRollback(RESOURCE_CRN, null)));
        assertEquals("Rollback of rotation failed at REDBEAMS_ROTATE_POLLING step regarding secret null, " +
                        "reason: Stack 'resourceCrn' not found.", secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfDatabaseServerCrnNotFound() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(new Cluster());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForRollback(RESOURCE_CRN, null)));
        assertEquals("Rollback of rotation failed at REDBEAMS_ROTATE_POLLING step regarding secret null, " +
                        "reason: No database server found for cluster: resourceCrn", secretRotationException.getMessage());
    }

    @Test
    void rollbackShouldThrowSecretRotationExceptionIfExternalCallFails() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        doThrow(new RuntimeException("error")).when(externalDatabaseService)
                .rotateDatabaseSecret(eq(DATABASE_SERVER_CRN), eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.ROLLBACK), any());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRollback(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForRollback(RESOURCE_CRN, null)));
        assertEquals("Rollback of rotation failed at REDBEAMS_ROTATE_POLLING step regarding secret null, reason: error",
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
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.ROLLBACK), any());
    }

    @Test
    void finalizeRotationShouldThrowSecretRotationExceptionIfStackNotFound() {
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenThrow(NotFoundException.notFound("Stack", RESOURCE_CRN).get());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForFinalize(RESOURCE_CRN, null)));
        assertEquals("Finalization of rotation failed at REDBEAMS_ROTATE_POLLING step regarding secret null, " +
                        "reason: Stack 'resourceCrn' not found.", secretRotationException.getMessage());
    }

    @Test
    void finalizeRotationShouldThrowSecretRotationExceptionIfDatabaseServerCrnNotFound() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(stackDto.getCluster()).thenReturn(new Cluster());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForFinalize(RESOURCE_CRN, null)));
        assertEquals("Finalization of rotation failed at REDBEAMS_ROTATE_POLLING step regarding secret null, " +
                        "reason: No database server found for cluster: resourceCrn", secretRotationException.getMessage());
    }

    @Test
    void finalizeRotationShouldThrowSecretRotationExceptionIfExternalCallFails() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        doThrow(new RuntimeException("error")).when(externalDatabaseService)
                .rotateDatabaseSecret(eq(DATABASE_SERVER_CRN), eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.FINALIZE), any());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD),
                        RotationMetadataTestUtil.metadataForFinalize(RESOURCE_CRN, null)));
        assertEquals("Finalization of rotation failed at REDBEAMS_ROTATE_POLLING step regarding secret null, reason: error",
                secretRotationException.getMessage());
    }

    @Test
    void finalizeRotationShouldSucceed() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        underTest.executeFinalize(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), null);
        verify(stackDtoService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(externalDatabaseService, times(1)).rotateDatabaseSecret(eq(DATABASE_SERVER_CRN),
                eq(REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD), eq(RotationFlowExecutionType.FINALIZE), any());
    }

    @Test
    void preValidateShouldSucceed() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        Cluster cluster = new Cluster();
        cluster.setDatabaseServerCrn(DATABASE_SERVER_CRN);
        when(stackDto.getCluster()).thenReturn(cluster);
        underTest.preValidate(new PollerRotationContext(RESOURCE_CRN, REDBEAMS_EXTERNAL_DATABASE_ROOT_PASSWORD));
        verify(stackDtoService, times(1)).getByCrn(eq(RESOURCE_CRN));
        verify(externalDatabaseService, times(1)).preValidateDatabaseSecretRotation(eq(DATABASE_SERVER_CRN));
    }

}