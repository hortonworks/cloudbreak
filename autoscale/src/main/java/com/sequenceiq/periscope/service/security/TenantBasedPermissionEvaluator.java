package com.sequenceiq.periscope.service.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.Clustered;

@Service
@Lazy
public class TenantBasedPermissionEvaluator implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantBasedPermissionEvaluator.class);

    @Inject
    @Lazy
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    @Lazy
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    @Lazy
    private CloudbreakAuthorizationService cloudbreakAuthorizationService;

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        if (!authentication.isAuthenticated()) {
            return true;
        }
        if (target instanceof Optional) {
            target = ((Optional<?>) target).orElse(null);
        }
        if (target == null) {
            return false;
        }
        CloudbreakUser cloudbreakUser = restRequestThreadLocalService.getCloudbreakUser();
        Collection<?> targets = target instanceof Collection ? (Collection<?>) target : Collections.singleton(target);
        return targets.stream().allMatch(t -> {
            if (!(t instanceof Clustered)) {
                return true;
            }
            Cluster cluster = ((Clustered) t).getCluster();
            if (cluster == null || !cloudbreakUser.getTenant().contentEquals(cluster.getClusterPertain().getTenant())) {
                return false;
            }
            cloudbreakAuthorizationService.hasAccess(cluster.getStackId(), cloudbreakUser.getUserId(), cloudbreakUser.getTenant(), permission.toString());
            return true;
        });
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }
}
