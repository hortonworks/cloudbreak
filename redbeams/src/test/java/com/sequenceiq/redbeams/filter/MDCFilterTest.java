package com.sequenceiq.redbeams.filter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.redbeams.service.ThreadBasedRequestIdProvider;

import java.io.IOException;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class MDCFilterTest {

    private static final String REQUEST_ID = "requestId1";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private MDCFilter underTest;

    @Mock
    private ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    @Mock
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Before
    public void setUp() {
        initMocks(this);

        underTest = new MDCFilter(threadBasedRequestIdProvider, threadBasedUserCrnProvider);
    }

    @Test
    public void testDoFilterInternal() throws IOException, ServletException {
        when(threadBasedRequestIdProvider.getRequestId()).thenReturn(REQUEST_ID);
        when(threadBasedUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);

        underTest.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        // highly coupled verification :(
        Map<String, String> mdcMap = MDCBuilder.getMdcContextMap();
        assertEquals(REQUEST_ID, mdcMap.get(LoggerContextKey.REQUEST_ID.toString()));
        assertEquals(USER_CRN, mdcMap.get(LoggerContextKey.USER_CRN.toString()));
    }

}
