package com.sequenceiq.periscope.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.periscope.service.AuditService;

public class AuditFilter extends OncePerRequestFilter {

    private static final Pattern PATTERN = Pattern.compile(".*/v1/distrox/?(.*)");

    private final AuditService auditService;

    private final AuthenticatedUserService authenticatedUserService;

    private final Boolean auditEnabled;

    public AuditFilter(Boolean auditEnabled, AuditService auditService, AuthenticatedUserService authenticatedUserService) {
        this.auditEnabled = auditEnabled;
        this.auditService = auditService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        if (auditEnabled && includePathPattern(request.getRequestURI())) {
            Map<String, Object> requestParameters = new HashMap<>();
            requestParameters.put("uri", request.getRequestURI());
            requestParameters.putAll(request.getParameterMap());
            boolean mutating = Set.of("POST", "PUT", "DELETE").contains(request.getMethod());
            String sourceip = Optional.ofNullable(request.getHeader("x-real-ip")).orElse(request.getRemoteAddr());
            String userAgent = Optional.ofNullable(request.getHeader("user-agent")).orElse("");
            auditService.auditRestApi(requestParameters, mutating, userAgent,
                    cloudbreakUser.getUserCrn(), cloudbreakUser.getTenant(), sourceip);
        }
    }

    protected boolean includePathPattern(String requestPath) {
        Matcher matcher = PATTERN.matcher(requestPath);
        if (matcher.matches()) {
            return true;
        }
        return false;
    }
}
