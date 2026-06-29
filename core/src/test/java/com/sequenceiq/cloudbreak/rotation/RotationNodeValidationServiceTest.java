package com.sequenceiq.cloudbreak.rotation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class RotationNodeValidationServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:accountId:datalake:resourceId";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackDto stackDto;

    @Mock
    private InstanceMetadataView runningInstance;

    @Mock
    private InstanceMetadataView runningInstance2;

    @Mock
    private InstanceMetadataView runningInstance3;

    @Mock
    private InstanceMetadataView stoppedInstance;

    @Mock
    private InstanceMetadataView nullFqdnInstance;

    @InjectMocks
    private RotationNodeValidationService underTest;

    @Test
    void validateNoStoppedInstancesShouldPassWhenAllInstancesAreRunning() {
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(runningInstance.isStopped()).thenReturn(false);
        when(stackDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(runningInstance));

        assertDoesNotThrow(() -> underTest.validateNoStoppedInstances(RESOURCE_CRN, CloudbreakSecretType.PRIVATE_HOST_CERTS));
    }

    @Test
    void validateNoStoppedInstancesShouldThrowWhenStoppedInstanceExists() {
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(stoppedInstance.isStopped()).thenReturn(true);
        when(stoppedInstance.getDiscoveryFQDN()).thenReturn("worker0.example.com");
        when(stackDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(runningInstance, stoppedInstance));

        SecretRotationException exception = assertThrows(SecretRotationException.class,
                () -> underTest.validateNoStoppedInstances(RESOURCE_CRN, CloudbreakSecretType.PRIVATE_HOST_CERTS));
        assertTrue(exception.getMessage().contains("stopped instances"));
        assertTrue(exception.getMessage().contains("worker0.example.com"));
        assertTrue(exception.getMessage().contains("PRIVATE_HOST_CERTS"));
    }

    @Test
    void validateNoStoppedInstancesShouldPassWhenMultipleInstancesAreAllRunning() {
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(runningInstance.isStopped()).thenReturn(false);
        when(runningInstance2.isStopped()).thenReturn(false);
        when(runningInstance3.isStopped()).thenReturn(false);
        when(stackDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(runningInstance, runningInstance2, runningInstance3));

        assertDoesNotThrow(() -> underTest.validateNoStoppedInstances(RESOURCE_CRN, CloudbreakSecretType.CM_INTERMEDIATE_CA_CERT));
    }

    @Test
    void validateNoStoppedInstancesShouldIgnoreStoppedInstanceWithNullFqdn() {
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(nullFqdnInstance.isStopped()).thenReturn(true);
        when(nullFqdnInstance.getDiscoveryFQDN()).thenReturn(null);
        when(stackDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(runningInstance, nullFqdnInstance));

        assertDoesNotThrow(() -> underTest.validateNoStoppedInstances(RESOURCE_CRN, CloudbreakSecretType.PRIVATE_HOST_CERTS));
    }

    @Test
    void validateNoStoppedInstancesShouldThrowWhenMixOfRunningAndStoppedInstances() {
        when(stackDtoService.getByCrn(RESOURCE_CRN)).thenReturn(stackDto);
        when(runningInstance.isStopped()).thenReturn(false);
        when(runningInstance2.isStopped()).thenReturn(false);
        when(stoppedInstance.isStopped()).thenReturn(true);
        when(stoppedInstance.getDiscoveryFQDN()).thenReturn("worker0.example.com");
        when(stackDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(runningInstance, stoppedInstance, runningInstance2));

        SecretRotationException exception = assertThrows(SecretRotationException.class,
                () -> underTest.validateNoStoppedInstances(RESOURCE_CRN, CloudbreakSecretType.PRIVATE_HOST_CERTS));
        assertTrue(exception.getMessage().contains("stopped instances"));
        assertTrue(exception.getMessage().contains("worker0.example.com"));
    }

    @Test
    void validateNoStoppedInstancesWithStackDtoShouldPassWhenAllRunning() {
        when(runningInstance.isStopped()).thenReturn(false);
        when(stackDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(runningInstance));

        assertDoesNotThrow(() -> underTest.validateNoStoppedInstances(stackDto, CloudbreakSecretType.USER_KEYPAIR));
    }

    @Test
    void validateNoStoppedInstancesWithStackDtoShouldThrowWhenStoppedExists() {
        when(stoppedInstance.isStopped()).thenReturn(true);
        when(stoppedInstance.getDiscoveryFQDN()).thenReturn("worker1.example.com");
        when(stackDto.getNotDeletedInstanceMetaData()).thenReturn(List.of(stoppedInstance));

        SecretRotationException exception = assertThrows(SecretRotationException.class,
                () -> underTest.validateNoStoppedInstances(stackDto, CloudbreakSecretType.USER_KEYPAIR));
        assertTrue(exception.getMessage().contains("worker1.example.com"));
        assertTrue(exception.getMessage().contains("USER_KEYPAIR"));
    }
}
