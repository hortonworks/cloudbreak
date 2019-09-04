package com.sequenceiq.cloudbreak.service.security;

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

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticationService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.TenantAwareResource;

@Service
@Lazy
public class TenantBasedPermissionEvaluator implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantBasedPermissionEvaluator.class);

    @Inject
    private AuthenticationService authenticationService;

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        if (target instanceof Optional) {
            target = ((Optional<?>) target).orElse(null);
        }
        if (target == null) {
            return false;
        }

        if (authentication == null) {
            return false;
        }

        CloudbreakUser user = authenticationService.getCloudbreakUser(authentication);
        Collection<?> targets = target instanceof Collection ? (Collection<?>) target : Collections.singleton(target);
        return targets.stream().allMatch(t -> hasPermission(user, t));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }

    //CHECKSTYLE:OFF
    private boolean hasPermission(CloudbreakUser cbUser, Object targetDomainObject) {
        Optional<Tenant> tenant = getTenant(targetDomainObject);
        return tenant.isPresent() && tenant.get().getName().contentEquals(cbUser.getTenant());
    }
    //CHECKSTYLE:ON

    private Optional<Tenant> getTenant(Object targetDomainObject) {
        if (targetDomainObject instanceof TenantAwareResource) {
            TenantAwareResource tenantAwareResource = (TenantAwareResource) targetDomainObject;
            return Optional.of(tenantAwareResource.getTenant());
        }
        return Optional.empty();
    }

}

