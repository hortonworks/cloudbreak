package com.sequenceiq.periscope.filter;

import static com.sequenceiq.cloudbreak.common.request.HeaderConstants.REQUEST_ID_HEADER;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.periscope.api.AutoscaleApi;
import com.sequenceiq.periscope.service.AuditService;

public class AuditFilter extends OncePerRequestFilter {

    private static final Pattern V1_DISTROX_PATH_PATTERN = Pattern.compile(".*" + AutoscaleApi.API_ROOT_CONTEXT + "/v1/distrox/?(.*)");

    private static final Pattern ALL_VERSIONED_PATH_PATTERN = Pattern.compile(".*" + AutoscaleApi.API_ROOT_CONTEXT + "/v?(.*)");

    private final AuditService auditService;

    private final AuthenticatedUserService authenticatedUserService;

    private final Boolean auditEnabled;

    private final boolean auditAllEndpointsEnabled;

    public AuditFilter(Boolean auditEnabled, AuditService auditService, AuthenticatedUserService authenticatedUserService, boolean auditAllEndpointsEnabled) {
        this.auditEnabled = auditEnabled;
        this.auditService = auditService;
        this.authenticatedUserService = authenticatedUserService;
        this.auditAllEndpointsEnabled = auditAllEndpointsEnabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser(request);
        if (auditEnabled && includePathPattern(request.getRequestURI())) {
            Map<String, Object> requestParameters = new HashMap<>();
            requestParameters.put("uri", request.getRequestURI());
            requestParameters.put("method", request.getMethod());
            requestParameters.putAll(request.getParameterMap());
            boolean mutating = Set.of("POST", "PUT", "DELETE").contains(request.getMethod());
            String sourceIp = Optional.ofNullable(request.getHeader("x-real-ip")).orElse(request.getRemoteAddr());
            String userAgent = Optional.ofNullable(request.getHeader("user-agent")).orElse("");
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            auditService.auditRestApi(requestParameters, mutating, userAgent,
                    cloudbreakUser, requestId, sourceIp);
        }
    }

    protected boolean includePathPattern(String requestPath) {
        boolean result = false;
        if (auditAllEndpointsEnabled) {
            result = patternMatches(ALL_VERSIONED_PATH_PATTERN, requestPath);
        } else {
            result = patternMatches(V1_DISTROX_PATH_PATTERN, requestPath);
        }
        return result;
    }

    private boolean patternMatches(Pattern pattern, String requestPath) {
        Matcher matcher = pattern.matcher(requestPath);
        return matcher.matches();
    }
}
