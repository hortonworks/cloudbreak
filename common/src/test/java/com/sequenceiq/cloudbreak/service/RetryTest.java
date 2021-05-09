package com.sequenceiq.cloudbreak.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RetryTest {

    private static final String MESSAGE = "message";

    @Test
    void actionFailedExceptionConstructorTestWhenDefault() {
        Retry.ActionFailedException underTest = new Retry.ActionFailedException();

        assertThat(underTest.getMessage()).isNull();
        assertThat(underTest.getCause()).isNull();
    }

    @Test
    void actionFailedExceptionConstructorTestWhenMessage() {
        Retry.ActionFailedException underTest = new Retry.ActionFailedException(MESSAGE);

        assertThat(underTest.getMessage()).isEqualTo(MESSAGE);
        assertThat(underTest.getCause()).isNull();
    }

    @Test
    void actionFailedExceptionConstructorTestWhenMessageAndCause() {
        Throwable cause = new RuntimeException();

        Retry.ActionFailedException underTest = new Retry.ActionFailedException(MESSAGE, cause);

        assertThat(underTest.getMessage()).isEqualTo(MESSAGE);
        assertThat(underTest.getCause()).isSameAs(cause);
    }

    @Test
    void actionFailedExceptionConstructorTestWhenCause() {
        Throwable cause = new RuntimeException(MESSAGE);

        Retry.ActionFailedException underTest = new Retry.ActionFailedException(cause);

        assertThat(underTest.getMessage()).isEqualTo("java.lang.RuntimeException: message");
        assertThat(underTest.getCause()).isSameAs(cause);
    }

    @Test
    void actionFailedExceptionConstructorTestWhenMessageAndCauseAndEnableSuppressionAndWritableStackTrace() {
        Throwable cause = new RuntimeException();
        Throwable suppressed = new RuntimeException();

        Retry.ActionFailedException underTest = new Retry.ActionFailedException(MESSAGE, cause, false, false);
        underTest.addSuppressed(suppressed);

        assertThat(underTest.getMessage()).isEqualTo(MESSAGE);
        assertThat(underTest.getCause()).isSameAs(cause);
        assertThat(underTest.getSuppressed()).isEmpty();
        assertThat(underTest.getStackTrace()).isEmpty();
    }

    @Test
    void actionFailedExceptionOfCauseTestWhenNull() {
        Retry.ActionFailedException underTest = Retry.ActionFailedException.ofCause(null);

        assertThat(underTest.getMessage()).isNull();
        assertThat(underTest.getCause()).isNull();
    }

    @Test
    void actionFailedExceptionOfCauseTestWhenNotNull() {
        Throwable cause = new RuntimeException(MESSAGE);

        Retry.ActionFailedException underTest = Retry.ActionFailedException.ofCause(cause);

        assertThat(underTest.getMessage()).isEqualTo(MESSAGE);
        assertThat(underTest.getCause()).isSameAs(cause);
    }

}