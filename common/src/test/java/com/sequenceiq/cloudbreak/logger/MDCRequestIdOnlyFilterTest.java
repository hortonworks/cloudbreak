package com.sequenceiq.cloudbreak.logger;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.util.UuidUtil;

@ExtendWith(MockitoExtension.class)
class MDCRequestIdOnlyFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Captor
    private ArgumentCaptor<MDCRequestIdOnlyFilter.RequestIdHeaderInjectingHttpRequestWrapper> wrapperArgumentCaptor;

    @InjectMocks
    private MDCRequestIdOnlyFilter underTest;

    @BeforeEach
    public void init() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("post");
    }

    @Test
    void getRequestIdFromHeaderWhenAlreadyPresent() throws ServletException, IOException {
        when(request.getHeader(REQUEST_ID_HEADER)).thenReturn("1234-5678-9999-8888");

        underTest.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(wrapperArgumentCaptor.capture(), any());

        verify(request, times(2)).getHeader(REQUEST_ID_HEADER);
        assertEquals("1234-5678-9999-8888", wrapperArgumentCaptor.getValue().getHeader(REQUEST_ID_HEADER));
    }

    @Test
    void generateRequestIdWhenMissing() throws ServletException, IOException {
        when(request.getHeader(REQUEST_ID_HEADER)).thenReturn(null);

        underTest.doFilterInternal(request, response, filterChain);
        verify(filterChain).doFilter(wrapperArgumentCaptor.capture(), any());

        verify(request, times(2)).getHeader(REQUEST_ID_HEADER);
        assertTrue(UuidUtil.isValid(wrapperArgumentCaptor.getValue().getHeader(REQUEST_ID_HEADER)));
    }
}
