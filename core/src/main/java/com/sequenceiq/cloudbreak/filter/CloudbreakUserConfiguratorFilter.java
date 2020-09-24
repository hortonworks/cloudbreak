package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;

public class CloudbreakUserConfiguratorFilter extends OncePerRequestFilter {

    private final LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    private final AuthenticatedUserService authenticatedUserService;

    public CloudbreakUserConfiguratorFilter(LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService,
            AuthenticatedUserService authenticatedUserService) {
        this.legacyRestRequestThreadLocalService = legacyRestRequestThreadLocalService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        legacyRestRequestThreadLocalService.setCloudbreakUser(cloudbreakUser);
        filterChain.doFilter(request, response);
        legacyRestRequestThreadLocalService.removeCloudbreakUser();
    }
}
