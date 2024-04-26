package com.sequenceiq.freeipa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.freeipa.entity.StackEncryption;
import com.sequenceiq.freeipa.repository.StackEncryptionRepository;

@ExtendWith(MockitoExtension.class)
class StackEncryptionServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private StackEncryptionRepository stackEncryptionRepository;

    @InjectMocks
    private StackEncryptionService underTest;

    @Test
    public void testSave() {
        StackEncryption stackEncryption = mock(StackEncryption.class);
        underTest.save(stackEncryption);
        verify(stackEncryptionRepository).save(stackEncryption);
    }

    @Test
    public void testDeleteStackEncryption() {
        underTest.deleteStackEncryption(STACK_ID);
        verify(stackEncryptionRepository).deleteStackEncryptionByStackId(STACK_ID);
    }

    @Test
    public void testGetStackEncryption() {
        StackEncryption stackEncryption = mock(StackEncryption.class);
        when(stackEncryptionRepository.findStackEncryptionByStackId(STACK_ID)).thenReturn(Optional.of(stackEncryption));
        StackEncryption result = underTest.getStackEncryption(STACK_ID);
        assertEquals(stackEncryption, result);
    }

    @Test
    public void testGetStackEncryptionThrowsException() {
        when(stackEncryptionRepository.findStackEncryptionByStackId(STACK_ID)).thenReturn(Optional.empty());
        CloudbreakServiceException exception = assertThrows(CloudbreakServiceException.class,
                () -> underTest.getStackEncryption(STACK_ID));
        assertEquals("Stack Encryption does not exist", exception.getMessage());
    }
}