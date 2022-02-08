package com.sequenceiq.cloudbreak.usage.http;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.concurrent.BlockingDeque;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.streaming.model.StreamProcessingException;

@ExtendWith(MockitoExtension.class)
public class UsageHttpRecordWorkerTest {

    private UsageHttpRecordWorker underTest;

    @Mock
    private UsageHttpRecordProcessor recordProcessor;

    @Mock
    private BlockingDeque<UsageHttpRecordRequest> processingQueue;

    @Mock
    private UsageHttpConfiguration configuration;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @BeforeEach
    public void setUp() {
        underTest = new UsageHttpRecordWorker("sample", "service", recordProcessor, processingQueue, configuration, null);
    }

    @Test
    public void testProcessRecordInput() throws Exception {
        // GIVEN
        underTest = spy(underTest);
        doReturn(httpClient).when(underTest).getHttpClient();
        given(httpClient.<String>send(any(), any())).willReturn(httpResponse);
        given(configuration.getEndpoint()).willReturn("http://endpoint");
        given(httpResponse.statusCode()).willReturn(Response.Status.OK.getStatusCode());
        // WHEN
        underTest.processRecordInput(createRequest());
        // THEN
        verify(httpResponse, times(1)).statusCode();
    }

    @Test
    public void testProcessRecordInputWithBadStatus() throws Exception {
        // GIVEN
        underTest = spy(underTest);
        doReturn(httpClient).when(underTest).getHttpClient();
        given(httpClient.<String>send(any(), any())).willReturn(httpResponse);
        given(configuration.getEndpoint()).willReturn("http://endpoint");
        given(httpResponse.statusCode()).willReturn(Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
        // WHEN
        StreamProcessingException exception = assertThrows(StreamProcessingException.class, () -> underTest.processRecordInput(createRequest()));
        // THEN
        assertTrue(exception.getMessage().contains("Usage could not be uploaded to http://endpoint (status code: 503)"));
    }

    @Test
    public void testProcessRecordInputWithEmptyPayload() throws Exception {
        // GIVEN
        UsageHttpRecordRequest emptyRequest = new UsageHttpRecordRequest(null, null, new Date().getTime(), true);
        // WHEN
        StreamProcessingException exception = assertThrows(StreamProcessingException.class, () -> underTest.processRecordInput(emptyRequest));
        // THEN
        assertTrue(exception.getMessage().contains("Raw body payload is missing from the usage request"));
    }

    @Test
    public void testProcessRecordInputWithUnexpectedError() throws Exception {
        // GIVEN
        underTest = spy(underTest);
        given(configuration.getEndpoint()).willReturn("http://endpoint");
        doReturn(httpClient).when(underTest).getHttpClient();
        doThrow(new IOException("unexpected error")).when(httpClient).<String>send(any(), any());
        // WHEN
        StreamProcessingException exception = assertThrows(StreamProcessingException.class, () -> underTest.processRecordInput(createRequest()));
        // THEN
        assertTrue(exception.getMessage().contains("unexpected error"));
    }

    private UsageHttpRecordRequest createRequest() {
        return new UsageHttpRecordRequest("{}", null, new Date().getTime(), true);
    }
}
