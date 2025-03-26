package com.sequenceiq.cloudbreak.rotation.service.history;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.rotation.common.TestSecretType;
import com.sequenceiq.cloudbreak.rotation.entity.SecretRotationHistory;
import com.sequenceiq.cloudbreak.rotation.repository.SecretRotationHistoryRepository;
import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

@ExtendWith(MockitoExtension.class)
public class SecretRotationHistoryServiceTest {

    @Mock
    private SecretRotationHistoryRepository repository;

    @InjectMocks
    private SecretRotationHistoryService underTest;

    @Test
    void testUpdateHistoryIfPresent() {
        when(repository.findByResourceCrnAndSecretType(any(), any())).thenReturn(
                Optional.of(new SecretRotationHistory("crn", TestSecretType.TEST, 1L)));

        underTest.addHistoryItem(RotationMetadata.builder().build());

        verify(repository).save(any());
    }

    @Test
    void testUpdateHistoryIfNotPresent() {
        when(repository.findByResourceCrnAndSecretType(any(), any())).thenReturn(Optional.empty());

        underTest.addHistoryItem(RotationMetadata.builder().build());

        verify(repository).save(any());
    }
}
