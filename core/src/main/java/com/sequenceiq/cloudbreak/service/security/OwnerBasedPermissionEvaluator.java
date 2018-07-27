package com.sequenceiq.cloudbreak.service.security;

import java.io.Serializable;
import java.lang.reflect.Field;
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
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.aspect.PermissionType;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.repository.security.UserRepository;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;

@Service
@Lazy
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OwnerBasedPermissionEvaluator.class);

    private static final String AUTO_SCALE_SCOPE = "cloudbreak.autoscale";

    @Inject
    @Lazy
    private CachedUserDetailsService cachedUserDetailsService;

    @Inject
    private UserRepository userRepository;

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
            return oauth.getOAuth2Request().getScope().contains(AUTO_SCALE_SCOPE);
        }

        IdentityUser user = cachedUserDetailsService.getDetails((String) authentication.getPrincipal(), UserFilterField.USERNAME);

        // TODO: implement this properly:
//        User cbUser = userRepository.findByEmail(user.getUserId());
//        Set<UserOrgPermissions> organizations = cbUser.getOrganizations();

        Collection<?> targets = target instanceof Collection ? (Collection<?>) target : Collections.singleton(target);
        return targets.stream().allMatch(t -> {
            try {
                // TODO: check if organization of the resource is in the list of the user's organizations
                // TODO: also check by role: organizations.iterator().next().getRole()
                return hasPermission(user, p, t);
            } catch (IllegalAccessException e) {
                LOGGER.error("Object doesn't have properties to check permission with class: " + t.getClass().getCanonicalName(), e);
                return false;
            }
        });
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }

    private boolean hasPermission(IdentityUser user, PermissionType p, Object targetDomainObject) throws IllegalAccessException {
        return getOwner(targetDomainObject).equals(user.getUserId()) || getAccount(targetDomainObject).equals(user.getAccount())
                && (user.getRoles().contains(IdentityUserRole.ADMIN) || (p == PermissionType.READ && isPublicInAccount(targetDomainObject)));
    }

    private String getAccount(Object targetDomainObject) throws IllegalAccessException {
        String result = "";
        Field accountField = ReflectionUtils.findField(targetDomainObject.getClass(), "account");
        if (accountField != null) {
            accountField.setAccessible(true);
            result = (String) accountField.get(targetDomainObject);
        }
        return result;
    }

    private boolean isPublicInAccount(Object targetDomainObject) throws IllegalAccessException {
        Field publicInAccountField = ReflectionUtils.findField(targetDomainObject.getClass(), "publicInAccount");
        if (publicInAccountField != null) {
            publicInAccountField.setAccessible(true);
            return (Boolean) publicInAccountField.get(targetDomainObject);
        }
        return false;
    }

    private String getOwner(Object targetDomainObject) throws IllegalAccessException {
        String result = "";
        Field ownerField = ReflectionUtils.findField(targetDomainObject.getClass(), "owner");
        if (ownerField != null) {
            ownerField.setAccessible(true);
            result = (String) ownerField.get(targetDomainObject);
        }
        return result;
    }
}
