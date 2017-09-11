package com.sequenceiq.periscope.service.security;

import java.io.Serializable;
import java.lang.reflect.Field;

import javax.inject.Inject;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.service.NotFoundException;

@Service
@Lazy
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    @Inject
    @Lazy
    private UserDetailsService userDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
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

    private String getUserId(Object targetDomainObject) throws IllegalAccessException {
        Field clusterField = ReflectionUtils.findField(targetDomainObject.getClass(), "cluster");
        if (clusterField != null) {
            clusterField.setAccessible(true);
            Cluster cluster = (Cluster) clusterField.get(targetDomainObject);
            return getUserId(cluster);
        } else {
            Field userIdField = ReflectionUtils.findField(targetDomainObject.getClass(), "userId");
            if (userIdField != null) {
                userIdField.setAccessible(true);
                return (String) userIdField.get(targetDomainObject);
            }
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
