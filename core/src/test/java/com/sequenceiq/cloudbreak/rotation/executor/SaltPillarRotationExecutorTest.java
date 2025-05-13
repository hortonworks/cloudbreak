package com.sequenceiq.cloudbreak.rotation.executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.rotation.RotationMetadataTestUtil;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.SaltPillarRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.notification.SecretRotationNotificationService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@ExtendWith(MockitoExtension.class)
class SaltPillarRotationExecutorTest {

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final Long CLUSTER_ID = 1L;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private SecretRotationSaltService saltService;

    @Mock
    private SecretRotationNotificationService secretRotationNotificationService;

    @InjectMocks
    private SaltPillarRotationExecutor underTest;

    @Test
    void validationShouldSucceed() {
        mockStackDto();
        doNothing().when(saltService).validateSaltPrimaryGateway(any());

        underTest.executePreValidation(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null);

        verify(saltService).validateSaltPrimaryGateway(any());
    }

    @Test
    void validationShouldFailIfSaltPingFails() {
        mockStackDto();
        doThrow(new SecretRotationException("pingpong")).when(saltService).validateSaltPrimaryGateway(any());

        assertThrows(SecretRotationException.class, () ->
                underTest.executePreValidation(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null));

        verify(saltService).validateSaltPrimaryGateway(any());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfStackNotFound() {
        when(stackDtoService.getByCrn(eq(RESOURCE_CRN))).thenThrow(NotFoundException.notFound("Stack", RESOURCE_CRN).get());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()),
                        RotationMetadataTestUtil.metadataForRotation(RESOURCE_CRN, null)));
        assertEquals("Execution of rotation failed at SALT_PILLAR step regarding secret null, reason: Stack 'resourceCrn' not found.",
                secretRotationException.getMessage());
    }

    @Test
    void rotateShouldThrowSecretRotationExceptionIfSaltFails() throws CloudbreakOrchestratorFailedException {
        mockStackDto();
        doThrow(new RuntimeException("error")).when(saltService).updateSaltPillar(any(), any());
        SecretRotationException secretRotationException = assertThrows(SecretRotationException.class,
                () -> underTest.executeRotate(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()),
                        RotationMetadataTestUtil.metadataForRotation(RESOURCE_CRN, null)));
        assertEquals("Execution of rotation failed at SALT_PILLAR step regarding secret null, reason: error",
                secretRotationException.getMessage());
        verify(saltService, times(1)).updateSaltPillar(any(), any());
    }

    @Test
    void rotateShouldSucceed() throws Exception {
        doNothing().when(saltService).updateSaltPillar(any(), any());
        mockStackDto();
        underTest.executeRotate(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null);
        verify(saltService, times(1)).updateSaltPillar(any(), any());
    }

    @Test
    void rollbackShouldSucceed() throws Exception {
        doNothing().when(saltService).updateSaltPillar(any(), any());
        mockStackDto();
        underTest.executeRollback(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null);
        verify(saltService, times(1)).updateSaltPillar(any(), any());
    }

    @Test
    void finalizeRotationShouldDoNothing() throws CloudbreakOrchestratorFailedException {
        underTest.executeFinalize(new SaltPillarRotationContext(RESOURCE_CRN, getSaltPillarGenerator()), null);
        verify(saltService, never()).updateSaltPillar(any(), any());
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