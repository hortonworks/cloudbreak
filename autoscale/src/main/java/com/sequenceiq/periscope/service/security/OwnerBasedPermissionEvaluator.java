package com.sequenceiq.periscope.service.security;

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
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.PeriscopeUser;

@Service
@Lazy
public class OwnerBasedPermissionEvaluator implements PermissionEvaluator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OwnerBasedPermissionEvaluator.class);

    @Inject
    @Lazy
    private CachedUserDetailsService cachedUserDetailsService;

    @Override
    public boolean hasPermission(Authentication authentication, Object target, Object permission) {
        if (target instanceof Optional) {
            target = ((Optional) target).orElse(null);
        }
        if (target == null) {
            return false;
        }
        PeriscopeUser user = cachedUserDetailsService.getDetails((String) authentication.getPrincipal(), UserFilterField.USERNAME);
        Collection<?> targets = target instanceof Collection ? (Collection<?>) target : Collections.singleton(target);
        return targets.stream().allMatch(t -> {
            try {
                return getUserId(t).equals(user.getId());
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
