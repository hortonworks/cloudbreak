package com.sequenceiq.cloudbreak.cloud.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.service.Retry;

class CloudExceptionConverterTest {

    private CloudExceptionConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new CloudExceptionConverter();
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenNull() {
        CloudConnectorException result = underTest.convertToCloudConnectorException(null, "Checking resources");

        verifyCloudConnectorException(result, null, "Checking resources failed: null");
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenActionFailedExceptionWithNoCause() {
        Throwable e = new Retry.ActionFailedException("Serious problem");

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        verifyCloudConnectorException(result, e, "Checking resources failed: Serious problem");
    }

    private void verifyCloudConnectorException(CloudConnectorException cloudConnectorException, Throwable causeExpected, String messageExpected) {
        assertThat(cloudConnectorException).isNotNull();
        assertThat(cloudConnectorException).hasMessage(messageExpected);
        assertThat(cloudConnectorException).hasCauseReference(causeExpected);
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenActionFailedExceptionWithCauseGeneralThrowable() {
        Throwable cause = new UnsupportedOperationException("Serious problem #2");
        Throwable e = new Retry.ActionFailedException("Serious problem #1", cause);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        verifyCloudConnectorException(result, cause, "Checking resources failed: Serious problem #2");
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenActionFailedExceptionWithCauseActionFailedException() {
        Throwable cause = new Retry.ActionFailedException("Serious problem #2");
        Throwable e = new Retry.ActionFailedException("Serious problem #1", cause);

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        verifyCloudConnectorException(result, cause, "Checking resources failed: Serious problem #2");
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenGeneralThrowable() {
        Throwable e = new UnsupportedOperationException("Serious problem");

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        verifyCloudConnectorException(result, e, "Checking resources failed: Serious problem");
    }

    @Test
    void convertToCloudConnectorExceptionTestWhenCloudConnectorException() {
        Throwable e = new CloudConnectorException("Serious problem");

        CloudConnectorException result = underTest.convertToCloudConnectorException(e, "Checking resources");

        assertThat(result).isSameAs(e);
    }

}