package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.domain.security.UserOrgPermissions;
import com.sequenceiq.cloudbreak.service.user.UserOrgPermissionsService;
import com.sequenceiq.cloudbreak.validation.OrganizationPermissions;
import com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Action;
import com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Resource;

@Component
public class PermissionCheckingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckingUtils.class);

    @Inject
    private UserOrgPermissionsService userOrgPermissionsService;

    public void checkPermissionsByTarget(Object target, User user, Resource resource, Action action) {
        Iterable<?> iterableTarget = targetToIterable(target);
        validateTarget(iterableTarget);
        Set<Long> organizationIds = collectOrganizationIds((Iterable<OrganizationResource>) iterableTarget);
        Set<UserOrgPermissions> userOrgPermissionSet = userOrgPermissionsService.findForUserByOrganizationIds(user, organizationIds);
        if (userOrgPermissionSet.size() != organizationIds.size()) {
            throw new AccessDeniedException("You have no access to this resource.");
        }
        userOrgPermissionSet.stream()
                .map(UserOrgPermissions::getPermissionSet)
                .forEach(permissions -> checkPermissionByPermissionSet(action, resource, permissions));
    }

    private Iterable<?> targetToIterable(Object target) {
        return target instanceof Iterable<?> ? (Iterable<?>) target : Collections.singletonList(target);
    }

    private void validateTarget(Iterable<?> iterableTarget) {
        Iterator<?> iterator = iterableTarget.iterator();
        if (iterator.hasNext()) {
            Object firstElement = iterator.next();
            if (!(firstElement instanceof OrganizationResource)) {
                throw new IllegalArgumentException(format("Type of target in Iterable<?> should be %s! It is instead: %s",
                        OrganizationResource.class.getCanonicalName(), firstElement));
            }
        }
    }

    private Set<Long> collectOrganizationIds(Iterable<OrganizationResource> target) {
        Iterable<OrganizationResource> iterableTarget = target;
        Set<Long> organizationIds = new HashSet<>();
        iterableTarget.forEach(organizationResource -> {
            Long organizationId = getOrganizationId(organizationResource);
            organizationIds.add(organizationId);
        });
        return organizationIds;
    }

    private Long getOrganizationId(OrganizationResource organizationResource) {
        Organization organization = organizationResource.getOrganization();
        if (organization == null) {
            throw new IllegalArgumentException("Organization cannot be null!");
        }
        Long organizationId = organization.getId();
        if (organizationId == null) {
            throw new IllegalArgumentException("OrganizationId cannot be null!");
        }
        return organizationId;
    }

    public Object checkPermissionsByPermissionSetAndProceed(Resource resource, User user, Long orgId, Action action,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {

        if (orgId == null) {
            throw new IllegalArgumentException("organizationId cannot be null!");
        }
        UserOrgPermissions userOrgPermissions = userOrgPermissionsService.findForUserByOrganizationId(user, orgId);
        if (userOrgPermissions == null) {
            throw new AccessDeniedException("You have no access to this resource.");
        }
        Set<String> permissionSet = userOrgPermissions.getPermissionSet();
        LOGGER.info("Checking {} permission for: {}", action, resource);
        checkPermissionByPermissionSet(action, resource, permissionSet);
        return proceed(proceedingJoinPoint, methodSignature);
    }

    public void checkPermissionByPermissionSet(Action action, Resource resource, Set<String> permissionSet) {
        boolean hasPermission = OrganizationPermissions.hasPermission(permissionSet, resource, action);
        if (!hasPermission) {
            throw new AccessDeniedException("You have no access to this resource.");
        }
    }

    public void validateIndex(int index, int length, String indexName) {
        if (index >= length) {
            throw new IllegalArgumentException(
                    format("The %s [%s] cannot be bigger than or equal to the methods argument count [%s]", indexName, index, length));
        }
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        try {
            Object proceed = proceedingJoinPoint.proceed();
            if (proceed == null) {
                LOGGER.info("Return value is null, method signature: {}", methodSignature.toLongString());
            }
            return proceed;
        } catch (Throwable t) {
            throw new IllegalStateException("Proceeding failed!", t);
        }
    }
}
