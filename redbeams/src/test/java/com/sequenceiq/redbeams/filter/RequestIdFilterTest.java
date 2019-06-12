package com.sequenceiq.redbeams.filter;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.redbeams.service.ThreadBasedRequestIdProvider;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

public class RequestIdFilterTest {

    private static final String REQUEST_ID = "requestId1";

    private RequestIdFilter underTest;

    @Mock
    private ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Before
    public void setUp() {
        initMocks(this);

        underTest = new RequestIdFilter(threadBasedRequestIdProvider);
    }

    @Test
    public void testDoFilterInternal() throws IOException, ServletException {
        when(request.getHeader("x-cdp-request-id")).thenReturn(REQUEST_ID);

        underTest.doFilterInternal(request, response, filterChain);

        InOrder inOrder = inOrder(threadBasedRequestIdProvider, filterChain);
        inOrder.verify(threadBasedRequestIdProvider).setRequestId(REQUEST_ID);
        inOrder.verify(filterChain).doFilter(request, response);
        inOrder.verify(threadBasedRequestIdProvider).removeRequestId();
    }

}
