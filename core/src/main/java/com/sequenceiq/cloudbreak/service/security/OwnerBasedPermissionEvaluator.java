package com.sequenceiq.cloudbreak.service.security;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;

@Service
@Lazy
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OwnerBasedPermissionEvaluator.class);

    private static final String AUTO_SCALE_SCOPE = "cloudbreak.autoscale";

    @Inject
    @Lazy
    private UserDetailsService userDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        Permission p = Permission.valueOf(permission.toString().toUpperCase());
        if (target == null) {
            throw new NotFoundException("Resource not found.");
        }
        OAuth2Authentication oauth = (OAuth2Authentication) authentication;
        if (oauth.getUserAuthentication() == null) {
            return oauth.getOAuth2Request().getScope().contains(AUTO_SCALE_SCOPE);
        }

        IdentityUser user = userDetailsService.getDetails((String) authentication.getPrincipal(), UserFilterField.USERNAME);
        Collection<?> targets = target instanceof Collection ? (Collection<?>) target : Collections.singleton(target);
        return targets.stream().allMatch(t -> {
            try {
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

    private boolean hasPermission(IdentityUser user, Permission p, Object targetDomainObject) throws IllegalAccessException {
        if (getOwner(targetDomainObject).equals(user.getUserId()) || getAccount(targetDomainObject).equals(user.getAccount())
                && (user.getRoles().contains(IdentityUserRole.ADMIN) || (p == Permission.READ && isPublicInAccount(targetDomainObject)))) {
            return true;
        }
        return false;
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

    private enum Permission {
        READ, WRITE
    }
}
