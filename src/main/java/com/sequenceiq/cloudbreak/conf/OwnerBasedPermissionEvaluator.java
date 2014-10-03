package com.sequenceiq.cloudbreak.conf;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;

@Component
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    private UserDetailsService userDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, final Object targetDomainObject, Object permission) {
        if (targetDomainObject == null) {
            return false;
        }
        try {
            String username = (String) authentication.getPrincipal();
            if (getOwner(targetDomainObject).equals(username)) {
                return true;
            }
            CbUser user = userDetailsService.getDetails(username);
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
