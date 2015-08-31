package com.sequenceiq.cloudbreak.conf;

import java.io.Serializable;
import java.lang.reflect.Field;

import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    private static final String AUTO_SCALE_SCOPE = "cloudbreak.autoscale";
    private UserDetailsService userDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, final Object targetDomainObject, Object permission) {
        if (targetDomainObject == null) {
            throw new NotFoundException("Resource not found.");
        }
        OAuth2Authentication oauth = (OAuth2Authentication) authentication;
        if (oauth.getUserAuthentication() == null && oauth.getOAuth2Request().getScope().contains(AUTO_SCALE_SCOPE)) {
            return true;
        }
        try {
            CbUser user = userDetailsService.getDetails((String) authentication.getPrincipal(), UserFilterField.USERNAME);
            if (getOwner(targetDomainObject).equals(user.getUserId())) {
                return true;
            }
            if (getAccount(targetDomainObject).equals(user.getAccount())) {
                if (user.getRoles().contains(CbUserRole.ADMIN) || isPublicInAccount(targetDomainObject)) {
                    return true;
                }
            }
        } catch (IllegalAccessException e) {
            return false;
        }
        return false;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        return false;
    }

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
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

    private Boolean isPublicInAccount(Object targetDomainObject) throws IllegalAccessException {
        Boolean result = false;
        Field publicInAccountField = ReflectionUtils.findField(targetDomainObject.getClass(), "publicInAccount");
        if (publicInAccountField != null) {
            publicInAccountField.setAccessible(true);
            result = (Boolean) publicInAccountField.get(targetDomainObject);
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
