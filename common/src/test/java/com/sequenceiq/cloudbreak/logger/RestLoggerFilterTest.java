package com.sequenceiq.cloudbreak.logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

@ExtendWith(MockitoExtension.class)
class RestLoggerFilterTest {

    private RestLoggerFilter underTest;

    @Mock
    private LogVerifier logger;

    @Mock
    private LoggingEventBuilder loggingEventBuilder;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    public void init() throws Exception {
        underTest = new RestLoggerFilter(true, logger);
    }

    @Test
    void doFilterCredential() throws ServletException, IOException {
        ArgumentCaptor<Level> requestArgumentCaptor = ArgumentCaptor.forClass(Level.class);

        when(request.getRequestURI()).thenReturn("env/credential");
        when(request.getMethod()).thenReturn("post");
        when(request.getCharacterEncoding()).thenReturn("UTF8");
        when(logger.atLevel(requestArgumentCaptor.capture())).thenReturn(loggingEventBuilder);
        underTest.doFilterInternal(request, response, filterChain);

        verify(loggingEventBuilder, times(1)).log(anyString());
        verify(loggingEventBuilder).log(contains("REDACTED COMPLETELY"));
        assertEquals(Level.DEBUG, requestArgumentCaptor.getValue());
    }

    @Test
    void doFilterOther() throws ServletException, IOException {
        ArgumentCaptor<Level> requestArgumentCaptor = ArgumentCaptor.forClass(Level.class);

        when(request.getRequestURI()).thenReturn("env/something");
        when(request.getMethod()).thenReturn("post");
        when(request.getCharacterEncoding()).thenReturn("UTF8");
        when(logger.atLevel(requestArgumentCaptor.capture())).thenReturn(loggingEventBuilder);
        underTest.doFilterInternal(request, response, filterChain);

        verify(loggingEventBuilder, times(1)).log(anyString());
        verify(loggingEventBuilder).log(not(contains("REDACTED COMPLETELY")));
        assertEquals(Level.DEBUG, requestArgumentCaptor.getValue());
    }

    @Test
    void doFilterWhenGetFailMustReturnDebugLogging() throws ServletException, IOException {
        ArgumentCaptor<Level> requestArgumentCaptor = ArgumentCaptor.forClass(Level.class);

        when(request.getRequestURI()).thenReturn("env/something");
        when(request.getMethod()).thenReturn("get");
        when(response.getStatus()).thenReturn(404);
        when(request.getCharacterEncoding()).thenReturn("UTF8");
        when(logger.atLevel(requestArgumentCaptor.capture())).thenReturn(loggingEventBuilder);
        underTest.doFilterInternal(request, response, filterChain);

        verify(loggingEventBuilder, times(1)).log(anyString());
        verify(loggingEventBuilder).log(not(contains("REDACTED COMPLETELY")));
        assertEquals(Level.DEBUG, requestArgumentCaptor.getValue());
    }

    @Test
    void doFilterWhenGetSuccessMustReturnDebugLoggingWithRedactedResponse() throws ServletException, IOException {
        ArgumentCaptor<Level> requestArgumentCaptor = ArgumentCaptor.forClass(Level.class);

        when(request.getRequestURI()).thenReturn("env/something");
        when(request.getMethod()).thenReturn("get");
        when(response.getStatus()).thenReturn(200);
        when(request.getCharacterEncoding()).thenReturn("UTF8");
        when(logger.atLevel(requestArgumentCaptor.capture())).thenReturn(loggingEventBuilder);
        underTest.doFilterInternal(request, response, filterChain);

        verify(loggingEventBuilder, times(1)).log(anyString());
        verify(loggingEventBuilder).log(contains("REDACTED COMPLETELY"));
        assertEquals(Level.DEBUG, requestArgumentCaptor.getValue());
    }

    @Test
    void doFilterUnsupportedEncoding() throws ServletException, IOException {
        ArgumentCaptor<Level> requestArgumentCaptor = ArgumentCaptor.forClass(Level.class);

        when(request.getRequestURI()).thenReturn("env/something");
        when(request.getMethod()).thenReturn("post");
        when(request.getCharacterEncoding()).thenReturn("WRONG");
        when(logger.atLevel(requestArgumentCaptor.capture())).thenReturn(loggingEventBuilder);
        underTest.doFilterInternal(request, response, filterChain);

        verify(loggingEventBuilder, times(1)).log(anyString());
        verify(loggingEventBuilder).log(contains("We were not able to encode the content"));
        assertEquals(Level.DEBUG, requestArgumentCaptor.getValue());
    }
}
