package com.sequenceiq.cloudbreak.cm.client.tracing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

class CmRequestLoggerInterceptorTest {

    @Test
    void whenGetMethodIsUsedNoLoggingShouldBeDone() throws IOException {
        Interceptor.Chain mockedChain = mock(Interceptor.Chain.class);
        Request mockedRequest = mock(Request.class);
        when(mockedRequest.method()).thenReturn("GET");
        HttpUrl mock = mock(HttpUrl.class);
        when(mock.encodedPath()).thenReturn("/api/v42/postTemplate");
        when(mockedRequest.url()).thenReturn(mock);
        when(mockedChain.request()).thenReturn(mockedRequest);
        CmRequestLoggerInterceptor interceptor = new CmRequestLoggerInterceptor();
        interceptor.intercept(mockedChain);
        verify(mockedRequest, never()).url();
    }

    @Test
    void whenPostMethodIsUsedBasicInfoShouldBeLogged() throws IOException {
        Interceptor.Chain mockedChain = mock(Interceptor.Chain.class);
        Request mockedRequest = mock(Request.class);
        Response mockedResponse = mock(Response.class);
        RequestBody mockedRequestBody = mock(RequestBody.class);
        when(mockedRequest.method()).thenReturn("POST");
        when(mockedRequest.body()).thenReturn(mockedRequestBody);
        HttpUrl mock = mock(HttpUrl.class);
        when(mock.encodedPath()).thenReturn("/api/v42/postTemplate");
        when(mockedRequest.url()).thenReturn(mock);
        when(mockedResponse.code()).thenReturn(200);
        when(mockedChain.request()).thenReturn(mockedRequest);
        when(mockedChain.proceed(any())).thenReturn(mockedResponse);
        CmRequestLoggerInterceptor interceptor = new CmRequestLoggerInterceptor();
        interceptor.intercept(mockedChain);
        verify(mockedRequest, times(2)).url();
        verify(mockedResponse, times(1)).code();
    }
}