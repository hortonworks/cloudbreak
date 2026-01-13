package com.sequenceiq.periscope.filter;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

                {"/as/api/v2/clusters/1", false},
                {"/as/api/v2/clusters/1/enable", false},
                {"/as/api/v2/clusters/2/disable", false},
                {"/as/api/v2/clusters/3/suspended", false},
                {"/as/api/v2/clusters/3/running", false},

                {"/as/health", false},
                {"/as/info", false},
                {"/as/metrics", false},
        });
    }

    private static Iterable<Object[]> patternMatchingOnAllEndpoints() {
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


                {"/as/api/v2/clusters/1", true},
                {"/as/api/v2/clusters/1/", true},
                {"/as/api/v2/clusters/1/enable", true},
                {"/as/api/v2/clusters/2/disable", true},
                {"/as/api/v2/clusters/3/suspended", true},
                {"/as/api/v2/clusters/3/running", true},

                {"/as/api/vNEWERTHANEVERYTHING/clusters/3/running", true},
                {"/as/api//v1333/distrox/", true},


                {"/as/health", false},
                {"/as/info", false},
                {"/as/metrics", false},
        });
    }

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest = new AuditFilter(true, auditService, authenticatedUserService, false);
    }

    @Test
    public void testDoFilterInternalWhenAuditAndMutating() throws Exception {
        CloudbreakUser user = new CloudbreakUser("userid", "usercrn", "username", "useremail", "usertenant");
        when(authenticatedUserService.getCbUser(any(HttpServletRequest.class)))
                .thenReturn(user);
        when(request.getRequestURI()).thenReturn("/as/api/v1/distrox/crn/testcrn/autoscale_config");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("x-real-ip")).thenReturn("127.0.0.1");
        when(request.getHeader("user-agent")).thenReturn("test-user-agent");
        when(request.getHeader(REQUEST_ID_HEADER)).thenReturn("requestId");

        underTest.doFilterInternal(request, response, filterChain);

        Map<String, Object> requestParameters = Map.of(
                "method", "POST",
                "uri", "/as/api/v1/distrox/crn/testcrn/autoscale_config");
        verify(auditService, times(1)).auditRestApi(eq(requestParameters), eq(true), eq("test-user-agent"),
                eq(user), eq("requestId"), eq("127.0.0.1"));
    }

    @Test
    public void testDoFilterInternalWhenAuditAndNotMutating() throws Exception {
        CloudbreakUser user = new CloudbreakUser("userid", "usercrn", "username", "useremail", "usertenant");
        when(authenticatedUserService.getCbUser(any(HttpServletRequest.class)))
                .thenReturn(user);
        when(request.getRequestURI()).thenReturn("/as/api/v1/distrox/crn/testcrn/autoscale_config");
        when(request.getMethod()).thenReturn("GET");
        when(request.getHeader("x-real-ip")).thenReturn("127.0.0.1");
        when(request.getHeader("user-agent")).thenReturn("test-user-agent");
        when(request.getHeader(REQUEST_ID_HEADER)).thenReturn("requestId");

        underTest.doFilterInternal(request, response, filterChain);

        Map<String, Object> requestParameters = Map.of(
                "method", "GET",
                "uri", "/as/api/v1/distrox/crn/testcrn/autoscale_config");
        verify(auditService, times(1)).auditRestApi(eq(requestParameters), eq(false), eq("test-user-agent"),
                eq(user), eq("requestId"), eq("127.0.0.1"));
    }

    @Test
    public void testDoFilterInternalWhenNotAnAuditUrl() throws Exception {
        when(authenticatedUserService.getCbUser(any(HttpServletRequest.class)))
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

    @ParameterizedTest(name = "{0}: testMatch(url: {1}, should match {2}")
    @MethodSource("patternMatchingOnAllEndpoints")
    public void testIncludePathPatternWithAuditEnabledOnAllEndpoints(String url, boolean match) {
        underTest = new AuditFilter(true, auditService, authenticatedUserService, true);
        if (match) {
            assertTrue(underTest.includePathPattern(url));
        } else {
            assertFalse(underTest.includePathPattern(url));
        }
    }
}
