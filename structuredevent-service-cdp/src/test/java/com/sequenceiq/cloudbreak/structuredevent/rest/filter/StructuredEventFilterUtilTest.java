package com.sequenceiq.cloudbreak.structuredevent.rest.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestRequestDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.rest.RestResponseDetails;
import com.sequenceiq.cloudbreak.structuredevent.rest.CDPRestCommonService;
import com.sequenceiq.cloudbreak.structuredevent.service.CDPDefaultStructuredEventClient;
import com.sequenceiq.cloudbreak.structuredevent.util.LoggingStream;
import com.sequenceiq.cloudbreak.structuredevent.util.RestFilterRequestBodyLogger;

@ExtendWith(MockitoExtension.class)
public class StructuredEventFilterUtilTest {

    @InjectMocks
    private StructuredEventFilterUtil underTest;

    @Mock
    private CDPDefaultStructuredEventClient structuredEventClient;

    @Mock
    private CDPRestCommonService restCommonService;

    @Mock
    private RepositoryBasedDataCollector dataCollector;

    @Mock
    private CdpOperationDetailsFactory cdpOperationDetailsFactory;

    @Test
    public void testLogInboundEntityWhenMarkSupportedAndContentLongerThenMax() throws IOException {
        int length = LoggingStream.MAX_CONTENT_LENGTH * 2;
        String generatedString = RandomStringUtils.random(length, true, true);

        ByteArrayInputStream bais = new ByteArrayInputStream(generatedString.getBytes());

        StringBuilder sb = new StringBuilder();
        InputStream actual = RestFilterRequestBodyLogger.logInboundEntity(sb, bais, Charset.defaultCharset(), true);

        assertEquals(bais, actual);
        assertTrue(sb.toString().endsWith("...more...\n"));
    }

    @Test
    public void testLogInboundEntityWhenMarkNotSupportedAndContentLongerThenMax() throws IOException {
        int length = LoggingStream.MAX_CONTENT_LENGTH * 2;
        String generatedString = RandomStringUtils.random(length, true, true);

        ByteArrayInputStream bais = new ByteArrayInputStream(generatedString.getBytes());
        DataInputStream inputStream = new DataInputStream(bais) {
            @Override
            public boolean markSupported() {
                return false;
            }
        };

        StringBuilder sb = new StringBuilder();
        InputStream actual = RestFilterRequestBodyLogger.logInboundEntity(sb, inputStream, Charset.defaultCharset(), true);

        assertNotEquals(inputStream, actual);
        assertTrue(sb.toString().endsWith("...more...\n"));
    }

    @Test
    public void testLogInboundEntityWhenMarkSupportedAndContentLessThenMax() throws IOException {
        int length = 20;
        String generatedString = RandomStringUtils.random(length, true, true);

        ByteArrayInputStream bais = new ByteArrayInputStream(generatedString.getBytes());

        StringBuilder sb = new StringBuilder();
        InputStream actual = RestFilterRequestBodyLogger.logInboundEntity(sb, bais, Charset.defaultCharset(), true);

        assertEquals(bais, actual);
        assertFalse(sb.toString().endsWith("...more...\n"));
        assertEquals(sb.length(), 21);
    }

    @Test
    public void testSendStructuredEventWhenRestParamIsEmptyMapThenParamsAreInvalid() {
        RestRequestDetails restRequestDetails = new RestRequestDetails();
        RestResponseDetails restResponseDetails = new RestResponseDetails();
        underTest.sendStructuredEvent(restRequestDetails, restResponseDetails, Collections.emptyMap(), 0L, null);

        verify(structuredEventClient, never()).sendStructuredEvent(any());
    }

    @Test
    public void testSendStructuredEventWhenRestParamIsNotEmptyMapButValuesAreNullThenParamsAreInvalid() {
        RestRequestDetails restRequestDetails = new RestRequestDetails();
        RestResponseDetails restResponseDetails = new RestResponseDetails();
        Map<String, String> restParams = new HashMap<>();
        restParams.put("key", null);
        underTest.sendStructuredEvent(restRequestDetails, restResponseDetails, restParams, 0L, null);

        verify(structuredEventClient, never()).sendStructuredEvent(any());
    }

    @Test
    public void testSendStructuredEventWhenRestParamsAreOkThenParamsAreValid() {
        RestRequestDetails restRequestDetails = new RestRequestDetails();
        RestResponseDetails restResponseDetails = new RestResponseDetails();
        Map<String, String> restParams = new HashMap<>();
        restParams.put("key", "val");
        underTest.sendStructuredEvent(restRequestDetails, restResponseDetails, restParams, 0L, null);

        verify(structuredEventClient).sendStructuredEvent(any());
    }
}
