package com.sequenceiq.periscope.filter;

import static com.sequenceiq.periscope.monitor.evaluator.ScalingConstants.UNINITIALIZED_WORKSPACE_ID;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.periscope.domain.ClusterPertain;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterPertainService;

public class CloudbreakUserConfiguratorFilter extends OncePerRequestFilter {

    private final AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    private final AuthenticatedUserService authenticatedUserService;

    private final ClusterPertainService clusterPertainService;

    public CloudbreakUserConfiguratorFilter(AutoscaleRestRequestThreadLocalService restRequestThreadLocalService,
            AuthenticatedUserService authenticatedUserService, ClusterPertainService clusterPertainService) {
        this.restRequestThreadLocalService = restRequestThreadLocalService;
        this.authenticatedUserService = authenticatedUserService;
        this.clusterPertainService = clusterPertainService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        CloudbreakUser cloudbreakUser = authenticatedUserService.getCbUser();
        Long workspaceId = UNINITIALIZED_WORKSPACE_ID;
        if (cloudbreakUser != null) {
            workspaceId = clusterPertainService.getClusterPertain(cloudbreakUser.getUserCrn())
                    .map(ClusterPertain::getWorkspaceId)
                    .orElse(UNINITIALIZED_WORKSPACE_ID);
        }

        restRequestThreadLocalService.setCloudbreakUser(cloudbreakUser, workspaceId);
        filterChain.doFilter(request, response);
        restRequestThreadLocalService.removeCloudbreakUser();
    }
}
