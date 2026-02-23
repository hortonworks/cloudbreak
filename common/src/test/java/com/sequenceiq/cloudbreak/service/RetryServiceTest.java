package com.sequenceiq.cloudbreak.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetryServiceTest {

    @Mock
    private Optional<RetryErrorPatterns> retryErrorPatternsOptional;

    @InjectMocks
    private RetryService underTest;

    @Mock
    private RetryErrorPatterns retryErrorPatterns;

    @BeforeEach
    void setUp() {
        lenient().when(retryErrorPatternsOptional.isEmpty()).thenReturn(true);
    }

    @Test
    void noPatterns() {
        when(retryErrorPatternsOptional.isEmpty()).thenReturn(true);
        String message = "whatever";

        assertThatThrownBy(() -> underTest.testWith1SecDelayMax5TimesAndMultiplier2WithCheckRetriable(thrownInSupplierWithCause(message)))
                .isInstanceOf(Retry.ActionFailedException.class)
                .cause()
                .hasMessage(message);
    }

    @Test
    void matchingPattern() {
        when(retryErrorPatternsOptional.isEmpty()).thenReturn(false);
        when(retryErrorPatternsOptional.get()).thenReturn(retryErrorPatterns);
        String message = "pattern";
        when(retryErrorPatterns.containsNonRetryableError("java.lang.RuntimeException: pattern. RuntimeException: pattern"))
                .thenReturn(true);

        assertThatThrownBy(() -> underTest.testWith1SecDelayMax5TimesAndMultiplier2WithCheckRetriable(thrownInSupplierWithCause(message)))
                .isInstanceOf(Retry.ActionFailedNonRetryableException.class)
                .cause()
                .hasMessage(message);
    }

    @Test
    void noCause() {
        assertThatThrownBy(() -> underTest.testWith1SecDelayMax5TimesAndMultiplier2WithCheckRetriable(thrownInSupplierWithoutCause()))
                .isInstanceOf(Retry.ActionFailedException.class)
                .hasNoCause();
    }

    @Test
    void nonMatchingPattern() {
        when(retryErrorPatternsOptional.isEmpty()).thenReturn(false);
        when(retryErrorPatternsOptional.get()).thenReturn(retryErrorPatterns);
        String message = "pattern";
        when(retryErrorPatterns.containsNonRetryableError("java.lang.RuntimeException: pattern. RuntimeException: pattern"))
                .thenReturn(false);

        assertThatThrownBy(() -> underTest.testWith1SecDelayMax5TimesAndMultiplier2WithCheckRetriable(thrownInSupplierWithCause(message)))
                .isInstanceOf(Retry.ActionFailedException.class)
                .cause()
                .hasMessage(message);
    }

    private Supplier<Void> thrownInSupplierWithoutCause() {
        return () -> {
            throw new Retry.ActionFailedException("nocause");
        };
    }

    private Supplier<Void> thrownInSupplierWithCause(String message) {
        return () -> {
            throw new Retry.ActionFailedException(new RuntimeException(message));
        };
    }

}
