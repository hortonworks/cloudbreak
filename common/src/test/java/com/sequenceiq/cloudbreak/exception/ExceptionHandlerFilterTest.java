package com.sequenceiq.cloudbreak.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExceptionHandlerFilterTest {

    private ExceptionHandlerFilter underTest;

    @Mock
    private ErrorResponseHandler errorResponseHandler;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void init() {
        underTest = new ExceptionHandlerFilter(errorResponseHandler);
    }

    @Test
    void testDoFilterInternalWhenNoError() throws IOException, ServletException {
        underTest.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(eq(request), eq(response));
        verify(errorResponseHandler, never()).handleErrorResponse(any(), any());
    }

    @Test
    void testDoFilterInternalWhenErrorOccurred() throws IOException, ServletException {
        RuntimeException exception = new RuntimeException("cica");
        doThrow(exception).when(filterChain).doFilter(eq(request), eq(response));
        underTest.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(eq(request), eq(response));
        verify(errorResponseHandler, times(1)).handleErrorResponse(eq(response), eq(exception));
    }
}