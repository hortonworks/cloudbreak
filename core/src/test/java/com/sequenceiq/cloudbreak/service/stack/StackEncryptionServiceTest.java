package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.StackEncryption;
import com.sequenceiq.cloudbreak.repository.StackEncryptionRepository;

@ExtendWith(MockitoExtension.class)
class StackEncryptionServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private StackEncryptionRepository stackEncryptionRepository;

    @InjectMocks
    private StackEncryptionService underTest;

    @Test
    void testSave() {
        StackEncryption stackEncryption = mock(StackEncryption.class);
        underTest.save(stackEncryption);
        verify(stackEncryptionRepository).save(stackEncryption);
    }

    @Test
    void testDeleteStackEncryption() {
        underTest.deleteStackEncryption(STACK_ID);
        verify(stackEncryptionRepository).deleteStackEncryptionByStackId(STACK_ID);
    }

    @Test
    void testGetStackEncryption() {
        StackEncryption stackEncryption = mock(StackEncryption.class);
        when(stackEncryptionRepository.findStackEncryptionByStackId(STACK_ID)).thenReturn(Optional.of(stackEncryption));
        StackEncryption result = underTest.getStackEncryption(STACK_ID);
        assertEquals(stackEncryption, result);
    }

    @Test
    void testGetStackEncryptionThrowsException() {
        when(stackEncryptionRepository.findStackEncryptionByStackId(STACK_ID)).thenReturn(Optional.empty());
        NotFoundException exception = assertThrows(NotFoundException.class, () -> underTest.getStackEncryption(STACK_ID));
        assertEquals("Stack Encryption does not exist", exception.getMessage());
    }

    @Test
    void testFindByStackId() {
        when(stackEncryptionRepository.findStackEncryptionByStackId(STACK_ID)).thenReturn(Optional.of(new StackEncryption()));
        Optional<StackEncryption> result = underTest.findByStackId(STACK_ID);
        assertTrue(result.isPresent());
    }

    @Test
    void testFindByStackIdNotFound() {
        when(stackEncryptionRepository.findStackEncryptionByStackId(STACK_ID)).thenReturn(Optional.empty());
        Optional<StackEncryption> result = underTest.findByStackId(STACK_ID);
        assertTrue(result.isEmpty());
    }
}