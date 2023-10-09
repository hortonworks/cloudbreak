package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;

public class CloudbreakUserConfiguratorFilter extends OncePerRequestFilter {

    private final UserService userService;

    private final LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    private final AuthenticatedUserService authenticatedUserService;

    public CloudbreakUserConfiguratorFilter(UserService userService, LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService,
            AuthenticatedUserService authenticatedUserService) {
        this.userService = userService;
        this.legacyRestRequestThreadLocalService = legacyRestRequestThreadLocalService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser(request);
        if (cloudbreakUser != null) {
            userService.getOrCreate(cloudbreakUser);
        }
        try {
            legacyRestRequestThreadLocalService.setCloudbreakUser(cloudbreakUser);
            filterChain.doFilter(request, response);
        } finally {
            legacyRestRequestThreadLocalService.removeCloudbreakUser();
        }
    }
}
