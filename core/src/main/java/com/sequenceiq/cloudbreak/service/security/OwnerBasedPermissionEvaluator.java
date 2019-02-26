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
import com.sequenceiq.cloudbreak.authorization.SpecialScopes;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;

@Service
@Lazy
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OwnerBasedPermissionEvaluator.class);

    @Inject
    @Lazy
    private CachedUserDetailsService cachedUserDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        PermissionType p = PermissionType.valueOf(permission.toString().toUpperCase());
        if (target instanceof Optional) {
            target = ((Optional<?>) target).orElse(null);
        }
        if (target == null) {
            return false;
        }
        OAuth2Authentication oauth = (OAuth2Authentication) authentication;
        if (oauth.getUserAuthentication() == null) {
            return oauth.getOAuth2Request().getScope().contains(SpecialScopes.AUTO_SCALE.getScope());
        }

        CloudbreakUser user = cachedUserDetailsService.getDetails((String) authentication.getPrincipal(), UserFilterField.USERNAME);
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

    //CHECKSTYLE:OFF
    private boolean hasPermission(CloudbreakUser user, PermissionType p, Object targetDomainObject) throws IllegalAccessException {
        String owner = getOwner(targetDomainObject);
        String account = getAccount(targetDomainObject);
        return owner == null && account == null
                || user.getUserId().equals(owner)
                || account.equals(user.getAccount()) && p == PermissionType.READ;
    }
    //CHECKSTYLE:ON

    private String getAccount(Object targetDomainObject) throws IllegalAccessException {
        String result = "";
        Field accountField = ReflectionUtils.findField(targetDomainObject.getClass(), "account");
        if (accountField != null) {
            accountField.setAccessible(true);
            result = (String) accountField.get(targetDomainObject);
        }
        return result;
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
