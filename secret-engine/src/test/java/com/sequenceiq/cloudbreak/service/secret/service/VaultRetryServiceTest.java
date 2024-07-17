package com.sequenceiq.cloudbreak.service.secret.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CancellationException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.VaultException;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.type.MetricType;
import com.sequenceiq.cloudbreak.service.secret.service.VaultRetryService.VaultRetryException;

@ExtendWith(MockitoExtension.class)
class VaultRetryServiceTest {

    @InjectMocks
    private VaultRetryService underTest;

    @Mock
    private MetricService metricService;

    @Test
    void testRetryWhenNoException() {
        String result = underTest.tryReadingVault(() -> "result");
        assertEquals("result", result);
        verify(metricService, never()).incrementMetricCounter(any(MetricType.class));
    }

    @Test
    void testRetryWhenVaultThrowsCancellationException() {
        CancellationException exception = assertThrows(CancellationException.class, () -> underTest.tryReadingVault(() -> {
            throw new CancellationException("error");
        }));
        assertEquals("error", exception.getMessage());
        verify(metricService, never()).incrementMetricCounter(any(MetricType.class));
    }

    @Test
    void testRetryWhenVaultTokenExpired() {
        VaultException exception = assertThrows(VaultException.class, () -> underTest.tryReadingVault(() -> {
            throw new VaultException("Status 403 Forbidden");
        }));
        assertEquals("Status 403 Forbidden", exception.getMessage());
        verify(metricService, never()).incrementMetricCounter(any(MetricType.class));
    }

    @Test
    void testRetryWhenVaultThrowsRuntimeException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.tryReadingVault(() -> {
            throw new RuntimeException("error");
        }));
        assertEquals("error", exception.getMessage());
        verify(metricService, never()).incrementMetricCounter(any(MetricType.class));
    }

    @Test
    void testRecoverVaultRetryException() {
        VaultRetryException exception = assertThrows(VaultRetryException.class,
                () -> underTest.recover(new VaultRetryException("error", new RuntimeException("error"), "read", MetricType.VAULT_READ_FAILED)));
        assertEquals("error", exception.getMessage());
        verify(metricService, times(1)).incrementMetricCounter(eq(MetricType.VAULT_READ_FAILED));
    }

    @Test
    void testRecoverNonRetryableRuntimeException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.recover(new RuntimeException("error")));
        assertEquals("error", exception.getMessage());
        verify(metricService, never()).incrementMetricCounter(any(MetricType.class));
    }
}