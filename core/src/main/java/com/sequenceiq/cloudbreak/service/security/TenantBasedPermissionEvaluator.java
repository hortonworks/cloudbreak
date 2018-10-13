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
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.PermissionType;
import com.sequenceiq.cloudbreak.authorization.SpecialScopes;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.TenantAwareResource;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;

@Service
@Lazy
public class TenantBasedPermissionEvaluator implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantBasedPermissionEvaluator.class);

    @Inject
    @Lazy
    private CachedUserDetailsService cachedUserDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        PermissionType p = PermissionType.valueOf(permission.toString().toUpperCase());
        if (target instanceof Optional) {
            target = ((Optional) target).orElse(null);
        }
        if (target == null) {
            return false;
        }
        OAuth2Authentication oauth = (OAuth2Authentication) authentication;
        if (oauth.getUserAuthentication() == null) {
            return oauth.getOAuth2Request().getScope().contains(SpecialScopes.AUTO_SCALE.getScope());
        }

        CloudbreakUser user = cachedUserDetailsService.getDetails((String) authentication.getPrincipal(),
                AuthenticatedUserService.getTenant(oauth), UserFilterField.USERNAME);
        Collection<?> targets = target instanceof Collection ? (Collection<?>) target : Collections.singleton(target);
        return targets.stream().allMatch(t -> hasPermission(user, p, t));
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }

    //CHECKSTYLE:OFF
    private boolean hasPermission(CloudbreakUser cbUser, PermissionType p, Object targetDomainObject) {
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

