package com.sequenceiq.periscope.service.security;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.service.NotFoundException;

@Component
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    private UserDetailsService userDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, final Object targetDomainObject, Object permission) {
        if (targetDomainObject == null) {
            throw new NotFoundException("Resource not found.");
        }
        try {
            PeriscopeUser user = userDetailsService.getDetails((String) authentication.getPrincipal(), UserFilterField.USERNAME);
            if (getUserId(targetDomainObject).equals(user.getId())) {
                return true;
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

    private String getUserId(Object targetDomainObject) throws IllegalAccessException {
        Field clusterField = ReflectionUtils.findField(targetDomainObject.getClass(), "cluster");
        if (clusterField != null) {
            clusterField.setAccessible(true);
            Cluster cluster = (Cluster) clusterField.get(targetDomainObject);
            return getUserId(cluster);
        } else {
            return getUserIdFromCluster(targetDomainObject);
        }
    }

    private String getUserIdFromCluster(Object targetDomainObject) throws IllegalAccessException {
        Field owner = ReflectionUtils.findField(targetDomainObject.getClass(), "user");
        owner.setAccessible(true);
        PeriscopeUser user = (PeriscopeUser) owner.get(targetDomainObject);
        return user.getId();
    }
}
