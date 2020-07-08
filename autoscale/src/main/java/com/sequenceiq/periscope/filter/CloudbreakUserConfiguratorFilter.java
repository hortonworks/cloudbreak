package com.sequenceiq.periscope.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;

public class CloudbreakUserConfiguratorFilter extends OncePerRequestFilter {

    private final AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    private final AuthenticatedUserService authenticatedUserService;

    public CloudbreakUserConfiguratorFilter(AutoscaleRestRequestThreadLocalService restRequestThreadLocalService,
            AuthenticatedUserService authenticatedUserService) {
        this.restRequestThreadLocalService = restRequestThreadLocalService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        restRequestThreadLocalService.setCloudbreakUser(cloudbreakUser);
        filterChain.doFilter(request, response);
        restRequestThreadLocalService.removeCloudbreakUser();
    }
}