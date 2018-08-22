package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

public class OrganizationConfiguratorFilter extends OncePerRequestFilter {

    private final RestRequestThreadLocalService restRequestThreadLocalService;

    private final OrganizationService organizationService;

    private final UserService userService;

    private final AuthenticatedUserService authenticatedUserService;

    public OrganizationConfiguratorFilter(RestRequestThreadLocalService restRequestThreadLocalService, OrganizationService organizationService,
            UserService userService, AuthenticatedUserService authenticatedUserService) {
        this.restRequestThreadLocalService = restRequestThreadLocalService;
        this.organizationService = organizationService;
        this.userService = userService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        if (requestURI.contains("/v3/")) {
            Long orgId = Long.valueOf(StringUtils.substringBetween(requestURI, "/v3/", "/"));
            restRequestThreadLocalService.setRequestedOrgId(orgId);
        } else {
            IdentityUser identityUser = authenticatedUserService.getCbUser();
            if (identityUser != null) {
                User user = userService.getOrCreate(identityUser);
                Organization organization = organizationService.getDefaultOrganizationForUser(user);
                if (organization == null) {
                    throw new NotFoundException("Organization not found");
                }
                restRequestThreadLocalService.setRequestedOrgId(organization.getId());
            }
        }
        filterChain.doFilter(request, response);
        restRequestThreadLocalService.removeRequestedOrgId();
    }
}
