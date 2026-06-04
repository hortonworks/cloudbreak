package com.sequenceiq.cloudbreak.cloud.azure.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.management.exception.ManagementException;
import com.sequenceiq.cloudbreak.cloud.azure.util.AzureExceptionHandler;

@ExtendWith(MockitoExtension.class)
class AzureRetryExecutorTest {

    private static final Duration INITIAL_BACKOFF = Duration.ofMillis(1);

    @Mock
    private AzureExceptionHandler azureExceptionHandler;

    @Mock
    private ManagementException managementException;

    private AzureRetryExecutor underTest;

    @BeforeEach
    void setUp() {
        underTest = new AzureRetryExecutor(azureExceptionHandler);
    }

    @Test
    void executeWithConcurrentWriteRetryWhenCallSucceedsImmediatelyThenReturnsResult() {
        String result = underTest.executeWithConcurrentWriteRetry(() -> "ok", () -> "existing", 3, INITIAL_BACKOFF);

        assertThat(result).isEqualTo("ok");
        verify(azureExceptionHandler, never()).isConcurrentWrite(any(ManagementException.class));
        verify(azureExceptionHandler, never()).handleException(any(), eq(null));
    }

    @Test
    void executeWithConcurrentWriteRetryWhenConcurrentWriteAndExistingFoundThenReturnsExisting() {
        when(azureExceptionHandler.isConcurrentWrite(managementException)).thenReturn(true);
        when(azureExceptionHandler.handleException(any(), eq(null))).thenReturn("existing");

        Supplier<String> failingCreate = () -> {
            throw managementException;
        };

        String result = underTest.executeWithConcurrentWriteRetry(failingCreate, () -> "existing", 3, INITIAL_BACKOFF);

        assertThat(result).isEqualTo("existing");
        verify(azureExceptionHandler, atLeast(1)).isConcurrentWrite(managementException);
        verify(azureExceptionHandler, times(1)).handleException(any(), eq(null));
    }

    @Test
    void executeWithConcurrentWriteRetryWhenConcurrentWriteAndNoExistingThenRetriesAndSucceeds() {
        when(azureExceptionHandler.isConcurrentWrite(managementException)).thenReturn(true);
        when(azureExceptionHandler.handleException(any(), eq(null))).thenReturn(null);

        AtomicInteger callCount = new AtomicInteger();
        Supplier<String> create = () -> {
            if (callCount.getAndIncrement() == 0) {
                throw managementException;
            }
            return "created";
        };

        String result = underTest.executeWithConcurrentWriteRetry(create, () -> null, 3, INITIAL_BACKOFF);

        assertThat(result).isEqualTo("created");
        assertThat(callCount.get()).isEqualTo(2);
        verify(azureExceptionHandler, atLeast(1)).isConcurrentWrite(managementException);
        verify(azureExceptionHandler, times(1)).handleException(any(), eq(null));
    }

    @Test
    void executeWithConcurrentWriteRetryWhenExistenceCheckThrowsThenRetriesAndSucceeds() {
        when(azureExceptionHandler.isConcurrentWrite(managementException)).thenReturn(true);
        when(azureExceptionHandler.handleException(any(), eq(null))).thenThrow(new RuntimeException("temporary get failure"));

        AtomicInteger callCount = new AtomicInteger();
        Supplier<String> create = () -> {
            if (callCount.getAndIncrement() == 0) {
                throw managementException;
            }
            return "created";
        };

        String result = underTest.executeWithConcurrentWriteRetry(create, () -> null, 3, INITIAL_BACKOFF);

        assertThat(result).isEqualTo("created");
        assertThat(callCount.get()).isEqualTo(2);
        verify(azureExceptionHandler, atLeast(1)).isConcurrentWrite(managementException);
        verify(azureExceptionHandler, times(1)).handleException(any(), eq(null));
    }

    @Test
    void executeWithConcurrentWriteRetryWhenNotConcurrentWriteThenThrowsWithoutRetry() {
        when(azureExceptionHandler.isConcurrentWrite(managementException)).thenReturn(false);

        AtomicInteger callCount = new AtomicInteger();
        Supplier<String> create = () -> {
            callCount.incrementAndGet();
            throw managementException;
        };

        assertThatThrownBy(() -> underTest.executeWithConcurrentWriteRetry(create, () -> null, 3, INITIAL_BACKOFF))
                .isSameAs(managementException);

        assertThat(callCount.get()).isEqualTo(1);
        verify(azureExceptionHandler, atLeast(1)).isConcurrentWrite(managementException);
        verify(azureExceptionHandler, never()).handleException(any(), eq(null));
    }

    @Test
    void executeWithConcurrentWriteRetryWhenConcurrentWritePersistsThenThrowsAfterMaxRetries() {
        when(azureExceptionHandler.isConcurrentWrite(managementException)).thenReturn(true);
        when(azureExceptionHandler.handleException(any(), eq(null))).thenReturn(null);

        AtomicInteger callCount = new AtomicInteger();
        Supplier<String> create = () -> {
            callCount.incrementAndGet();
            throw managementException;
        };

        int maxRetries = 2;
        assertThatThrownBy(() -> underTest.executeWithConcurrentWriteRetry(create, () -> null, maxRetries, INITIAL_BACKOFF))
                .isSameAs(managementException);

        assertThat(callCount.get()).isEqualTo(maxRetries + 1);
        verify(azureExceptionHandler, atLeast(1)).isConcurrentWrite(managementException);
        verify(azureExceptionHandler, times(maxRetries + 1)).handleException(any(), eq(null));
    }
}
