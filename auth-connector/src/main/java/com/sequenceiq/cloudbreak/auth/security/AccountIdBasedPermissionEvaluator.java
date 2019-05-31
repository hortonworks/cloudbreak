package com.sequenceiq.cloudbreak.auth.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticationService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;

@Lazy
@Component
public class AccountIdBasedPermissionEvaluator implements PermissionEvaluator {

    @Inject
    private AuthenticationService authService;

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

        CloudbreakUser user = authService.getCloudbreakUser(authentication);
        Collection<?> targets = target instanceof Collection ? (Collection<?>) target : Collections.singleton(target);
        return targets.stream().allMatch(t -> hasPermission(user, t));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }

    //CHECKSTYLE:OFF
    private boolean hasPermission(CloudbreakUser cbUser, Object targetDomainObject) {
        Optional<String> accountId = getAccountId(targetDomainObject);
        return accountId.isPresent() && accountId.get().contentEquals(cbUser.getTenant());
    }
    //CHECKSTYLE:ON

    private Optional<String> getAccountId(Object targetDomainObject) {
        if (targetDomainObject instanceof AuthResource) {
            AuthResource tenantAwareResource = (AuthResource) targetDomainObject;
            return Optional.of(tenantAwareResource.getAccountId());
        }
        return Optional.empty();
    }

}

