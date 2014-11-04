package com.sequenceiq.cloudbreak.conf;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Component
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    private static final String PERISCOPE_CLIENT = "periscope";
    private UserDetailsService userDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, final Object targetDomainObject, Object permission) {
        if (targetDomainObject == null) {
            return false;
        }
        OAuth2Authentication oauth = (OAuth2Authentication) authentication;
        if (oauth.getUserAuthentication() == null && oauth.getOAuth2Request().getClientId().equals(PERISCOPE_CLIENT)) {
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
        Field accountField = ReflectionUtils.findField(targetDomainObject.getClass(), "account");
        accountField.setAccessible(true);
        return (String) accountField.get(targetDomainObject);
    }

    private Boolean isPublicInAccount(Object targetDomainObject) throws IllegalAccessException {
        Field publicInAccountField = ReflectionUtils.findField(targetDomainObject.getClass(), "publicInAccount");
        publicInAccountField.setAccessible(true);
        return (Boolean) publicInAccountField.get(targetDomainObject);
    }

    private String getOwner(Object targetDomainObject) throws IllegalAccessException {
        Field ownerField = ReflectionUtils.findField(targetDomainObject.getClass(), "owner");
        ownerField.setAccessible(true);
        return (String) ownerField.get(targetDomainObject);
    }
}
