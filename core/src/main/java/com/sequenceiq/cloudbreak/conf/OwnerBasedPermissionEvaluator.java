package com.sequenceiq.cloudbreak.conf;

import java.io.Serializable;
import java.lang.reflect.Field;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Component
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OwnerBasedPermissionEvaluator.class);

    private static final String AUTO_SCALE_SCOPE = "cloudbreak.autoscale";

    @Inject
    private UserDetailsService userDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, final Object targetDomainObject, Object permission) {
        Permission p = Permission.valueOf(permission.toString().toUpperCase());
        if (targetDomainObject == null) {
            throw new NotFoundException("Resource not found.");
        }
        OAuth2Authentication oauth = (OAuth2Authentication) authentication;
        if (oauth.getUserAuthentication() == null) {
            return oauth.getOAuth2Request().getScope().contains(AUTO_SCALE_SCOPE);
        }
        try {
            CbUser user = userDetailsService.getDetails((String) authentication.getPrincipal(), UserFilterField.USERNAME);
            if (getOwner(targetDomainObject).equals(user.getUserId())) {
                return true;
            }
            if (getAccount(targetDomainObject).equals(user.getAccount())
                    && (user.getRoles().contains(CbUserRole.ADMIN) || (p == Permission.READ && isPublicInAccount(targetDomainObject)))) {
                return true;
            }
        } catch (IllegalAccessException e) {
            LOGGER.error("Object doesn't have properties to check permission with class: " + targetDomainObject.getClass().getCanonicalName(), e);
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
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
