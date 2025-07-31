package com.sequenceiq.cloudbreak.cm.client.tracing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

class CmRequestLoggerInterceptorTest {

    @Test
    void whenGetMethodIsUsedNoLoggingShouldBeDone() throws IOException {
        Interceptor.Chain mockedChain = mock(Interceptor.Chain.class);
        Request mockedRequest = mock(Request.class);
        when(mockedRequest.method()).thenReturn("GET");
        when(mockedRequest.urlString()).thenReturn("/api/v42/postTemplate");
        when(mockedChain.request()).thenReturn(mockedRequest);
        CmRequestLoggerInterceptor interceptor = new CmRequestLoggerInterceptor();
        interceptor.intercept(mockedChain);
        verify(mockedRequest, never()).urlString();
    }

    @Test
    void whenPostMethodIsUsedBasicInfoShouldBeLogged() throws IOException {
        Interceptor.Chain mockedChain = mock(Interceptor.Chain.class);
        Request mockedRequest = mock(Request.class);
        Response mockedResponse = mock(Response.class);
        RequestBody mockedRequestBody = mock(RequestBody.class);
        when(mockedRequest.method()).thenReturn("POST");
        when(mockedRequest.body()).thenReturn(mockedRequestBody);
        when(mockedRequest.urlString()).thenReturn("/api/v42/postTemplate");
        when(mockedResponse.code()).thenReturn(200);
        when(mockedChain.request()).thenReturn(mockedRequest);
        when(mockedChain.proceed(any())).thenReturn(mockedResponse);
        CmRequestLoggerInterceptor interceptor = new CmRequestLoggerInterceptor();
        interceptor.intercept(mockedChain);
        verify(mockedRequest, times(2)).urlString();
        verify(mockedResponse, times(1)).code();
    }
}