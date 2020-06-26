package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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

    @Test
    void rootCauseValidJsonMsgReturned() {
        String simplemessage = "{\"error_description\":\"message\",\"key\":\"value\"}";
        Exception rc = new Exception(simplemessage);
        Exception e = new Exception(rc);
        assertThat(underTest.extractErrorMessage(e)).isEqualTo("message");
    }
}
