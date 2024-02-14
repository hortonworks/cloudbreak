package com.sequenceiq.cloudbreak.common.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HeaderValueProviderTest {

    private static final String HEADER_NAME = "user-agent";

    private static final String HEADER_VALUE = "CDPTFPROVIDER/dev";

    private static final String FALLBACK_HEADER_NAME = "cdp-caller-id";

    private static final String DEFAULT_VALUE = "No Info";

    @Test
    @DisplayName("When the header key is present, the method should return its value")
    void testGetHeaderValueFromRequestContextForSingleEntryWhenHeaderKeyPresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HEADER_NAME)).thenReturn(HEADER_VALUE);
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        Optional<String> result = HeaderValueProvider.getHeaderValueFromRequestContext(HEADER_NAME);

        assertEquals(HEADER_VALUE, result.orElse(null));
    }

    @Test
    @DisplayName("When the header key is not present, the method should return an empty Optional")
    void testGetHeaderValueFromRequestContextForSingleEntryWhenHeaderKeyIsNotPresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(any())).thenReturn(null);
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        Optional<String> result = HeaderValueProvider.getHeaderValueFromRequestContext(HEADER_NAME);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("When the header key is present, the method should return its value")
    void testGetHeaderOrItsFallbackValueOrDefaultWhenHeaderKeyIsPresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(HEADER_NAME)).thenReturn(HEADER_VALUE);
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        String result = HeaderValueProvider.getHeaderOrItsFallbackValueOrDefault(HEADER_NAME, FALLBACK_HEADER_NAME, DEFAULT_VALUE);

        assertEquals(HEADER_VALUE, result);
    }

    @Test
    @DisplayName("When the header key is not present, but the fallback header key is, the method should return its value")
    void testGetHeaderOrItsFallbackValueOrDefaultWhenHeaderKeyIsNotPresentButFallbackHeaderKeyIs() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(FALLBACK_HEADER_NAME)).thenReturn(HEADER_VALUE);
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        String result = HeaderValueProvider.getHeaderOrItsFallbackValueOrDefault(HEADER_NAME, FALLBACK_HEADER_NAME, DEFAULT_VALUE);

        assertEquals(HEADER_VALUE, result);
    }

    @Test
    @DisplayName("When neither the header key nor the fallback header key are present, the method should return the default value")
    void testGetHeaderOrItsFallbackValueOrDefaultWhenNeitherHeaderKeyArePresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(any())).thenReturn(null);
        ServletRequestAttributes attributes = mock(ServletRequestAttributes.class);
        when(attributes.getRequest()).thenReturn(request);
        RequestContextHolder.setRequestAttributes(attributes);

        String result = HeaderValueProvider.getHeaderOrItsFallbackValueOrDefault(HEADER_NAME, FALLBACK_HEADER_NAME, DEFAULT_VALUE);

        assertEquals(DEFAULT_VALUE, result);
    }
}