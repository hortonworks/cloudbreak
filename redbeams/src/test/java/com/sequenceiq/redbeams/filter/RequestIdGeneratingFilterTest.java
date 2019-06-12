package com.sequenceiq.redbeams.filter;

import static com.sequenceiq.redbeams.filter.RequestIdGeneratingFilter.RequestIdHeaderInjectingHttpRequestWrapper.REQUEST_ID_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.redbeams.filter.RequestIdGeneratingFilter.RequestIdHeaderInjectingHttpRequestWrapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class RequestIdGeneratingFilterTest {

    private static final String REQUEST_ID = "requestId1";

    private RequestIdGeneratingFilter underTest;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Before
    public void setUp() {
        initMocks(this);

        underTest = new RequestIdGeneratingFilter();
    }

    @Test
    public void testDoFilterInternal() throws IOException, ServletException {
        underTest.doFilterInternal(request, response, filterChain);

        ArgumentCaptor<HttpServletRequest> captor = ArgumentCaptor.forClass(HttpServletRequest.class);
        verify(filterChain).doFilter(captor.capture(), eq(response));

        HttpServletRequest wrapper = captor.getValue();
        assertTrue(wrapper instanceof RequestIdHeaderInjectingHttpRequestWrapper);
        assertEquals(request, ((RequestIdHeaderInjectingHttpRequestWrapper) wrapper).getRequest());
    }

    @Test
    public void testWrapperInjecting() {
        when(request.getHeader(REQUEST_ID_HEADER)).thenReturn(null);
        when(request.getHeader("foo")).thenReturn("bar");
        when(request.getHeader("x")).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of("foo")));
        when(request.getHeaders("foo")).thenReturn(Collections.enumeration(List.of("bar")));
        when(request.getHeaders("x")).thenReturn(Collections.emptyEnumeration());

        HttpServletRequest wrapper = new RequestIdHeaderInjectingHttpRequestWrapper(request);

        String generatedRequestId = wrapper.getHeader(REQUEST_ID_HEADER);
        assertNotNull(generatedRequestId);
        assertNotEquals("bar", generatedRequestId);
        assertEquals("bar", wrapper.getHeader("foo"));
        assertNull(wrapper.getHeader("x"));

        List<String> headerNames = Collections.list(wrapper.getHeaderNames());
        assertTrue(headerNames.contains(REQUEST_ID_HEADER));
        assertTrue(headerNames.contains("foo"));
        assertFalse(headerNames.contains("x"));

        List<String> headerValues = Collections.list(wrapper.getHeaders(REQUEST_ID_HEADER));
        assertEquals(List.of(generatedRequestId), headerValues);
        headerValues = Collections.list(wrapper.getHeaders("foo"));
        assertEquals(List.of("bar"), headerValues);
        headerValues = Collections.list(wrapper.getHeaders("x"));
        assertTrue(headerValues.isEmpty());
    }

    @Test
    public void testWrapperNotInjecting() {
        when(request.getHeader(REQUEST_ID_HEADER)).thenReturn(REQUEST_ID);
        when(request.getHeader("foo")).thenReturn("bar");
        when(request.getHeader("x")).thenReturn(null);
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(List.of(REQUEST_ID_HEADER, "foo")));
        when(request.getHeaders(REQUEST_ID_HEADER)).thenReturn(Collections.enumeration(List.of(REQUEST_ID)));
        when(request.getHeaders("foo")).thenReturn(Collections.enumeration(List.of("bar")));
        when(request.getHeaders("x")).thenReturn(Collections.emptyEnumeration());

        HttpServletRequest wrapper = new RequestIdHeaderInjectingHttpRequestWrapper(request);

        String requestId = wrapper.getHeader(REQUEST_ID_HEADER);
        assertEquals(REQUEST_ID, requestId);
        assertEquals("bar", wrapper.getHeader("foo"));
        assertNull(wrapper.getHeader("x"));

        List<String> headerNames = Collections.list(wrapper.getHeaderNames());
        assertTrue(headerNames.contains(REQUEST_ID_HEADER));
        assertTrue(headerNames.contains("foo"));
        assertFalse(headerNames.contains("x"));

        List<String> headerValues = Collections.list(wrapper.getHeaders(REQUEST_ID_HEADER));
        assertEquals(List.of(requestId), headerValues);
        headerValues = Collections.list(wrapper.getHeaders("foo"));
        assertEquals(List.of("bar"), headerValues);
        headerValues = Collections.list(wrapper.getHeaders("x"));
        assertTrue(headerValues.isEmpty());
    }

}
