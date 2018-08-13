package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;
import com.sequenceiq.cloudbreak.repository.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.user.UserOrgPermissionsService;
import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action;

@Component
public class PermissionCheckingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckingUtils.class);

    @Inject
    private UserOrgPermissionsService userOrgPermissionsService;

    public void checkPermissionsByTarget(Object target, User user, OrganizationResource resource, Action action) {
        Iterable<?> iterableTarget = targetToIterable(target);
        Set<Long> organizationIds = collectOrganizationIds(iterableTarget);
        if (organizationIds.isEmpty()) {
            throw new AccessDeniedException(format("You have no access to %s.", resource.getReadableName()));
        }
        Set<UserOrgPermissions> userOrgPermissionSet = userOrgPermissionsService.findForUserByOrganizationIds(user, organizationIds);
        if (userOrgPermissionSet.size() != organizationIds.size()) {
            throw new AccessDeniedException(format("You have no access to %s.", resource.getReadableName()));
        }
        userOrgPermissionSet.stream()
                .map(UserOrgPermissions::getPermissionSet)
                .forEach(permissions -> checkPermissionByPermissionSet(action, resource, permissions));
    }

    private Iterable<?> targetToIterable(Object target) {
        return target instanceof Iterable<?> ? (Iterable<?>) target : Collections.singletonList(target);
    }

    private Set<Long> collectOrganizationIds(Iterable<?> target) {
        return StreamSupport.stream(target.spliterator(), false)
                .map(resource -> {
                    if (resource instanceof Optional && ((Optional<?>) resource).isPresent()) {
                        return (OrganizationAwareResource) ((Optional<?>) resource).get();
                    } else if (resource instanceof OrganizationAwareResource) {
                        return (OrganizationAwareResource) resource;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .map(this::getOrganizationId)
                .collect(Collectors.toSet());
    }

    private Long getOrganizationId(OrganizationAwareResource organizationResource) {
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

    public Object checkPermissionsByPermissionSetAndProceed(OrganizationResource resource, User user, Long orgId, Action action,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        if (orgId == null) {
            throw new IllegalArgumentException("organizationId cannot be null!");
        }
        UserOrgPermissions userOrgPermissions = userOrgPermissionsService.findForUserByOrganizationId(user, orgId);
        if (userOrgPermissions == null) {
            throw new AccessDeniedException(format("You have no access to %s.", resource.getReadableName()));
        }
        Set<String> permissionSet = userOrgPermissions.getPermissionSet();
        LOGGER.info("Checking {} permission for: {}", action, resource);
        checkPermissionByPermissionSet(action, resource, permissionSet);
        return proceed(proceedingJoinPoint, methodSignature);
    }

    public void checkPermissionByPermissionSet(Action action, OrganizationResource resource, Set<String> permissionSet) {
        boolean hasPermission = OrganizationPermissions.hasPermission(permissionSet, resource, action);
        if (!hasPermission) {
            throw new AccessDeniedException(format("You have no access to %s.", resource.getReadableName()));
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
            if (t.getCause() != null && t.getCause().getClass().equals(org.hibernate.exception.ConstraintViolationException.class)) {
                throw new AccessDeniedException("Access is denied.", t);
            } else {
                throw new AccessDeniedException(t.getMessage(), t);
            }
        }
    }

    public Optional<Class<?>> getRepositoryClass(ProceedingJoinPoint proceedingJoinPoint) {
        return Arrays.stream(proceedingJoinPoint.getTarget().getClass().getInterfaces())
                .filter(i -> Arrays.asList(i.getInterfaces()).contains(OrganizationResourceRepository.class))
                .findFirst();
    }

    public Optional<Annotation> getAnnotation(Optional<Class<?>> repositoryClass) {
        return repositoryClass.flatMap(repo -> Arrays.stream(repo.getAnnotations())
                .filter(a -> a.annotationType().equals(OrganizationResourceType.class))
                .findFirst());
    }
}
