package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationConfiguratorFilter.class);

    private final Pattern v3ResourcePattern = Pattern.compile(".*\\/v3\\/(\\d*)\\/.*");

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
        Matcher v3ResourceMatcher = v3ResourcePattern.matcher(requestURI);

        if (v3ResourceMatcher.matches()) {
            String orgIdString = v3ResourceMatcher.group(1);
            try {
                Long orgId = Long.valueOf(orgIdString);
                restRequestThreadLocalService.setRequestedOrgId(orgId);
            } catch (NumberFormatException e) {
                LOGGER.error(String.format("OrganizationID couldn't be parsed from the V3 request URI: %s", requestURI), e);
            }
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
