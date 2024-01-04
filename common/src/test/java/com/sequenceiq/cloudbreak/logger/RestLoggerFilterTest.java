package com.sequenceiq.cloudbreak.logger;

import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import com.sequenceiq.cloudbreak.util.StaticFieldManipulationTestHelper;

@ExtendWith(MockitoExtension.class)
class RestLoggerFilterTest {

    private RestLoggerFilter underTest;

    @Mock
    private Logger logger;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    public void init() throws Exception {
        StaticFieldManipulationTestHelper.setFinalStatic(RestLoggerFilter.class.getDeclaredField("LOGGER"), logger);
        underTest = new RestLoggerFilter(true);
    }

    @Test
    void doFilterCredential() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("env/credential");
        when(request.getCharacterEncoding()).thenReturn("UTF8");
        underTest.doFilterInternal(request, response, filterChain);

        verify(logger, times(1)).debug(any());
        verify(logger).debug(contains("REDACTED COMPLETELY"));
    }

    @Test
    void doFilterOther() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("env/something");
        when(request.getCharacterEncoding()).thenReturn("UTF8");
        underTest.doFilterInternal(request, response, filterChain);

        verify(logger, times(1)).debug(any());
        verify(logger).debug(not(contains("REDACTED COMPLETELY")));
    }

    @Test
    void doFilterUnsupportedEncoding() throws ServletException, IOException {
        when(request.getRequestURI()).thenReturn("env/something");
        when(request.getCharacterEncoding()).thenReturn("WRONG");
        underTest.doFilterInternal(request, response, filterChain);

        verify(logger, times(1)).debug(any());
        verify(logger).debug(contains("We were not able to encode the content"));
    }
}
