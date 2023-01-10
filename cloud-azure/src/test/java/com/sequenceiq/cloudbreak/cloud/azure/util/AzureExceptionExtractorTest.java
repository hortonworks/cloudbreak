package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.of;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AzureExceptionExtractorTest {

    private AzureExceptionExtractor underTest = new AzureExceptionExtractor();

    @Test
    void nullReturned() {
        assertThat(underTest.extractErrorMessage(null)).isNull();
    }

    @Test
    void emptyMsgNullReturned() {
        Exception e = new Exception();
        assertThat(underTest.extractErrorMessage(e)).isNull();
    }

    @Test
    void rootCauseMsgNullReturned() {
        Exception rc = new Exception();
        Exception e = new Exception(rc);
        assertThat(underTest.extractErrorMessage(e)).isNull();
    }

    @Test
    void rootCauseSimpleMsgReturned() {
        String simplemessage = "simplemessage";
        Exception rc = new Exception(simplemessage);
        Exception e = new Exception(rc);
        assertThat(underTest.extractErrorMessage(e)).isEqualTo(simplemessage);
    }

    @Test
    void rootCauseImpropperJsonMsgReturnedAsIs() {
        String simplemessage = "{\"key\":\"value\"}";
        Exception rc = new Exception(simplemessage);
        Exception e = new Exception(rc);
        assertThat(underTest.extractErrorMessage(e)).isEqualTo(simplemessage);
    }

    @ParameterizedTest
    @MethodSource("errorMessageProvider")
    void rootCauseValidJsonMsgReturned(String exceptionMessage, String extractedMessage) {
        Exception rc = new Exception(exceptionMessage);
        Exception e = new Exception(rc);
        assertThat(underTest.extractErrorMessage(e)).isEqualTo(extractedMessage);
    }

    public static Stream<Arguments> errorMessageProvider() {
        return Stream.of(
                of("{\"error_description\":\"message\",\"key\":\"value\"}", "error_description: message"),
                of("{\"error_description\":\"message\",\"error\":\"error\",\"error_uri\":\"error_uri\",\"key\":\"value\"}",
                        "error_description: message, error: error, error_uri: error_uri"),
                of("{\"error_description\":\"message\",\"error_uri\":\"error_uri\",\"key\":\"value\"}", "error_description: message, error_uri: error_uri"),
                of("{\"key\":\"value\"}", "{\"key\":\"value\"}"));
    }
}
