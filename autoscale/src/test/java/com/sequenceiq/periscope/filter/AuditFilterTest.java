package com.sequenceiq.periscope.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.periscope.service.AuditService;

@ExtendWith(MockitoExtension.class)
public class AuditFilterTest {

    private AuditFilter underTest;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AuditService auditService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest = new AuditFilter(true, auditService, authenticatedUserService);
    }

    @Test
    public void testDoFilterInternalWhenAuditAndMutating() throws Exception {
        when(authenticatedUserService.getCbUser())
                .thenReturn(new CloudbreakUser("userid", "usercrn", "username", "useremail", "usertenant"));
        when(request.getRequestURI()).thenReturn("/as/api/v1/distrox/crn/testcrn/autoscale_config");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("x-real-ip")).thenReturn("127.0.0.1");
        when(request.getHeader("user-agent")).thenReturn("test-user-agent");

        underTest.doFilterInternal(request, response, filterChain);

        verify(auditService, times(1)).auditRestApi(eq(Map.of("uri", "/as/api/v1/distrox/crn/testcrn/autoscale_config")), eq(true), eq("test-user-agent"),
                eq("usercrn"), eq("usertenant"), eq("127.0.0.1"));
    }

    @Test
    public void testDoFilterInternalWhenAuditAndNotMutating() throws Exception {
        when(authenticatedUserService.getCbUser())
                .thenReturn(new CloudbreakUser("userid", "usercrn", "username", "useremail", "usertenant"));
        when(request.getRequestURI()).thenReturn("/as/api/v1/distrox/crn/testcrn/autoscale_config");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("x-real-ip")).thenReturn("127.0.0.1");
        when(request.getHeader("user-agent")).thenReturn("test-user-agent");

        underTest.doFilterInternal(request, response, filterChain);

        verify(auditService, times(1)).auditRestApi(eq(Map.of("uri", "/as/api/v1/distrox/crn/testcrn/autoscale_config")), eq(false), eq("test-user-agent"),
                eq("usercrn"), eq("usertenant"), eq("127.0.0.1"));
    }

    @Test
    public void testDoFilterInternalWhenNotAnAuditUrl() throws Exception {
        when(authenticatedUserService.getCbUser())
                .thenReturn(new CloudbreakUser("userid", "usercrn", "username", "useremail", "usertenant"));
        when(request.getRequestURI()).thenReturn("/as/api/healthcheck");

        underTest.doFilterInternal(request, response, filterChain);
        verifyNoInteractions(auditService);
    }

    @ParameterizedTest(name = "{0}: testMatch(url: {1}, should match {2}")
    @MethodSource("patternMatching")
    public void testIncludePathPattern(String url, boolean match) {
        if (match) {
            assertTrue(underTest.includePathPattern(url));
        } else {
            assertFalse(underTest.includePathPattern(url));
        }
    }

    private static Iterable<Object[]> patternMatching() {
        return Arrays.asList(new Object[][]{
                {"/as/api/v1/distrox", true},
                {"/as/api/v1/distrox/", true},

                {"/as/api/v1/distrox/name/testcluster/autoscale_config", true},
                {"/as/api/v1/distrox/name/testcluster/autoscale_config/", true},
                {"/as/api/v1/distrox/name/testcluster/autoscale_config/test", true},

                {"/as/api/v1/distrox/name/testcluster/autoscale", true},
                {"/as/api/v1/distrox/name/testcluster/autoscale/", true},
                {"/as/api/v1/distrox/name/testcluster/autoscale/test", true},

                {"/as/api/v1/distrox/crn/testcrn/autoscale_config", true},
                {"/as/api/v1/distrox/crn/testcrn/autoscale_config/", true},
                {"/as/api/v1/distrox/crn/testcrn/autoscale_config/test", true},

                {"/as/api/v1/distrox/crn/testcrn/autoscale", true},
                {"/as/api/v1/distrox/crn/testcrn/autoscale/", true},
                {"/as/api/v1/distrox/crn/testcrn/autoscale/test", true},

                {"/as/api/v1/distrox/name/testcluster/history", true},
                {"/as/api/v1/distrox/crn/testcrn/history", true},
                {"/as/api/v1/distrox/name/testcluster/history/", true},
                {"/as/api/v1/distrox/crn/testcrn/history/", true},

                {"/as/health", false},
                {"/as/info", false},
                {"/as/metrics", false},
        });
    }
}
