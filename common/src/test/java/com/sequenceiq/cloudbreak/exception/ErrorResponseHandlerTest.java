package com.sequenceiq.cloudbreak.exception;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.cloudbreak.exception.mapper.DefaultExceptionMapper;

@ExtendWith(MockitoExtension.class)
class ErrorResponseHandlerTest {

    @Spy
    private List<BaseExceptionMapper<?>> exceptionMappers = new ArrayList<>();

    @Mock
    private BaseExceptionMapper exceptionMapper;

    @InjectMocks
    private ErrorResponseHandler errorResponseHandler;

    @BeforeEach
    void init() {
        lenient().when(exceptionMapper.getExceptionType()).thenReturn(RuntimeException.class);
        lenient().when(exceptionMapper.getResponseStatus(any())).thenReturn(Status.BAD_REQUEST);
        exceptionMappers.add(exceptionMapper);
        errorResponseHandler.init();
    }

    @Test
    void handleErrorResponseWhenExceptionMapperIsUnknown() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);
        errorResponseHandler.handleErrorResponse(response, new IllegalArgumentException());
        verify(response, times(1)).setStatus(Status.INTERNAL_SERVER_ERROR.getStatusCode());
        verify(response, times(1)).setContentType(MediaType.APPLICATION_JSON);
        verify(printWriter, times(1)).flush();
    }

    @Test
    void handleErrorResponseWhenExceptionMapperExists() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(printWriter);
        errorResponseHandler.handleErrorResponse(response, new RuntimeException());
        verify(response, times(1)).setStatus(Status.BAD_REQUEST.getStatusCode());
        verify(response, times(1)).setContentType(MediaType.APPLICATION_JSON);
        verify(printWriter, times(1)).flush();
    }

    @Test
    void testExceptionMapperMergingSelectsServiceSpecificMapperWhenCommonExceptionMapperIsFirstInTheInjectedList() throws IOException {
        exceptionMappers.clear();
        DefaultExceptionMapper commonExceptionMapper = new DefaultExceptionMapper();
        TestExceptionMapperNotInCommonPackage serviceExceptionMapper = new TestExceptionMapperNotInCommonPackage();
        exceptionMappers.add(commonExceptionMapper);
        exceptionMappers.add(serviceExceptionMapper);
        errorResponseHandler.init();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
        errorResponseHandler.handleErrorResponse(response, new IllegalStateException());
        verify(response, times(1)).setStatus(Status.BAD_REQUEST.getStatusCode());
    }

    @Test
    void testExceptionMapperMergingSelectsServiceSpecificMapperWhenCommonExceptionMapperIsSecondInTheInjectedList() throws IOException {
        exceptionMappers.clear();
        DefaultExceptionMapper commonExceptionMapper = new DefaultExceptionMapper();
        TestExceptionMapperNotInCommonPackage serviceExceptionMapper = new TestExceptionMapperNotInCommonPackage();
        exceptionMappers.add(serviceExceptionMapper);
        exceptionMappers.add(commonExceptionMapper);
        errorResponseHandler.init();
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(mock(PrintWriter.class));
        errorResponseHandler.handleErrorResponse(response, new IllegalStateException());
        verify(response, times(1)).setStatus(Status.BAD_REQUEST.getStatusCode());
    }

    private static class TestExceptionMapperNotInCommonPackage extends BaseExceptionMapper<IllegalStateException> {
        @Override
        public Status getResponseStatus(IllegalStateException exception) {
            return Status.BAD_REQUEST;
        }

        @Override
        public Class<IllegalStateException> getExceptionType() {
            return IllegalStateException.class;
        }
    }
}