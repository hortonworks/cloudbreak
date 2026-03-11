package com.sequenceiq.cloudbreak.rotation.service.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
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

    @Test
    void checkIfRotationDueReturnsTrueWhenNoHistoryAndCreationDateMakesNextDuePast() {
        when(repository.findByResourceCrnAndSecretType(any(), any())).thenReturn(Optional.empty());
        // Creation date treated as last rotation: created 20 days ago + 30 day interval = nextDue 10 days ago -> due
        Instant resourceCreationDate = Instant.now().minus(Duration.ofDays(20));
        boolean due = underTest.checkIfRotationDue("crn", TestSecretType.TEST, Duration.ofDays(30), resourceCreationDate);
        assertThat(due).isTrue();
    }

    @Test
    void checkIfRotationDueReturnsFalseWhenNoHistoryAndCreationDateMakesNextDueBeyondBuffer() {
        when(repository.findByResourceCrnAndSecretType(any(), any())).thenReturn(Optional.empty());
        // Creation date treated as last rotation: created 5 days ago + 30 day interval = nextDue 25 days in future -> not due
        Instant resourceCreationDate = Instant.now().minus(Duration.ofDays(5));
        boolean due = underTest.checkIfRotationDue("crn", TestSecretType.TEST, Duration.ofDays(30), resourceCreationDate);
        assertThat(due).isFalse();
    }

    @Test
    void checkIfRotationDueReturnsFalseWhenIntervalNullZeroOrNegative() {
        Instant resourceCreationDate = Instant.now();
        boolean nullInterval = underTest.checkIfRotationDue("crn", TestSecretType.TEST, null, resourceCreationDate);
        boolean zeroInterval = underTest.checkIfRotationDue("crn", TestSecretType.TEST, Duration.ZERO, resourceCreationDate);
        boolean negativeInterval = underTest.checkIfRotationDue("crn", TestSecretType.TEST, Duration.ofDays(-1), resourceCreationDate);
        assertThat(nullInterval).isFalse();
        assertThat(zeroInterval).isFalse();
        assertThat(negativeInterval).isFalse();
    }

    @Test
    void checkIfRotationDueReturnsTrueWhenExpiredAlready() {
        // lastUpdated so that nextDue = lastUpdated + interval <= now (expired)
        Duration interval = Duration.ofDays(30);
        long lastUpdatedMs = Instant.now().minus(interval).minus(Duration.ofHours(1)).toEpochMilli();
        when(repository.findByResourceCrnAndSecretType(any(), any()))
                .thenReturn(Optional.of(new SecretRotationHistory("crn", TestSecretType.TEST, lastUpdatedMs)));

        boolean due = underTest.checkIfRotationDue("crn", TestSecretType.TEST, interval, Instant.now());
        assertThat(due).isTrue();
    }

    @Test
    void checkIfRotationDueReturnsTrueWhenWithinTenDayBuffer() {
        // nextDue is 5 days in the future, within the 10-day buffer -> due
        Duration interval = Duration.ofDays(30);
        long lastUpdatedMs = Instant.now().minus(interval).plus(Duration.ofDays(5)).toEpochMilli();
        when(repository.findByResourceCrnAndSecretType(any(), any()))
                .thenReturn(Optional.of(new SecretRotationHistory("crn", TestSecretType.TEST, lastUpdatedMs)));

        boolean due = underTest.checkIfRotationDue("crn", TestSecretType.TEST, interval, Instant.now());
        assertThat(due).isTrue();
    }

    @Test
    void checkIfRotationDueReturnsFalseWhenBeyondTenDayBuffer() {
        // nextDue is 15 days in the future, beyond the 10-day buffer -> not due
        Duration interval = Duration.ofDays(30);
        long lastUpdatedMs = Instant.now().minus(interval).plus(Duration.ofDays(15)).toEpochMilli();
        when(repository.findByResourceCrnAndSecretType(any(), any()))
                .thenReturn(Optional.of(new SecretRotationHistory("crn", TestSecretType.TEST, lastUpdatedMs)));

        boolean due = underTest.checkIfRotationDue("crn", TestSecretType.TEST, interval, Instant.now());
        assertThat(due).isFalse();
    }
}
