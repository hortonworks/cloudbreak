package com.sequenceiq.cloudbreak.filter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.CachedWorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

public class WorkspaceConfiguratorFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceConfiguratorFilter.class);

    private final CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    private final CachedWorkspaceService workspaceService;

    private final UserService userService;

    private final AuthenticatedUserService authenticatedUserService;

    public WorkspaceConfiguratorFilter(CloudbreakRestRequestThreadLocalService restRequestThreadLocalService, CachedWorkspaceService workspaceService,
            UserService userService, AuthenticatedUserService authenticatedUserService) {
        this.restRequestThreadLocalService = restRequestThreadLocalService;
        this.workspaceService = workspaceService;
        this.userService = userService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser(request);
        try {
            if (cloudbreakUser != null) {
                if (ThreadBasedUserCrnProvider.getUserCrn() != null && !ThreadBasedUserCrnProvider.getUserCrn().equals(cloudbreakUser.getUserCrn())) {
                    LOGGER.debug("Before:There is a difference between:: Spring security context: {} and header-based-actor: '{}'",
                            cloudbreakUser.getUserCrn(), ThreadBasedUserCrnProvider.getUserCrn());
                    logHeadersSafely(request);
                }
                User user = userService.getOrCreate(cloudbreakUser);
                String accountId = Crn.fromString(cloudbreakUser.getUserCrn()).getAccountId();
                // default workspaceName is always the accountId
                Optional<Workspace> tenantDefaultWorkspace = workspaceService.getByName(accountId, user);
                if (!tenantDefaultWorkspace.isPresent()) {
                    throw new IllegalStateException("Tenant default workspace does not exist!");
                }
                if (ThreadBasedUserCrnProvider.getUserCrn() != null && !ThreadBasedUserCrnProvider.getUserCrn().equals(user.getUserCrn())) {
                    LOGGER.debug("Before:There is a difference between:: CB user context: {} and header-based-actor: '{}'",
                            user.getUserCrn(), ThreadBasedUserCrnProvider.getUserCrn());
                }
                Long workspaceId = tenantDefaultWorkspace.get().getId();
                restRequestThreadLocalService.setRequestedWorkspaceId(workspaceId);
                WorkspaceIdModifiedRequest modifiedRequest = new WorkspaceIdModifiedRequest(request, workspaceId);
                LOGGER.debug("Before:CloudbreakRestRequestThreadLocalContext: {}", restRequestThreadLocalService.getRestThreadLocalContextAsString());
                filterChain.doFilter(modifiedRequest, response);
                LOGGER.debug("After:CloudbreakRestRequestThreadLocalContext: {}", restRequestThreadLocalService.getRestThreadLocalContextAsString());
            } else {
                filterChain.doFilter(request, response);
            }
        } finally {
            restRequestThreadLocalService.removeRequestedWorkspaceId();
        }
    }

    private void logHeadersSafely(HttpServletRequest request) {
        try {
            Map<String, String> headers = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(request.getHeaderNames().asIterator(), Spliterator.ORDERED), false)
                    .collect(Collectors.toMap(headerName -> headerName, headerName -> getHeaderValues(request, headerName)));
            LOGGER.debug("HTTP headers: \n{}", Joiner.on("\n").withKeyValueSeparator(" | ").join(headers));
        } catch (Exception e) {
            LOGGER.debug("Failed to log HTTP headers, because: {}", e.getMessage());
        }
    }

    private String getHeaderValues(HttpServletRequest request, String headerName) {
        Set<String> headerValues = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                        request.getHeaders(headerName).asIterator(), Spliterator.ORDERED), false)
                .collect(Collectors.toSet());
        return Joiner.on(" , ").join(headerValues);
    }
}
