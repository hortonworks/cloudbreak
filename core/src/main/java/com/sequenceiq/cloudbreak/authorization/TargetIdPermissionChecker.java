package com.sequenceiq.cloudbreak.authorization;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByTargetId;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.organization.OrganizationAwareResource;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.repository.OrganizationResourceRepository;

@Component
public class TargetIdPermissionChecker implements PermissionChecker<CheckPermissionsByTargetId> {

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, OrganizationResource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionsByTargetId methodAnnotation = (CheckPermissionsByTargetId) rawMethodAnnotation;
        int targetIdIndex = methodAnnotation.targetIdIndex();
        int length = proceedingJoinPoint.getArgs().length;
        permissionCheckingUtils.validateIndex(targetIdIndex, length, "targetIdIndex");
        Optional<Class<?>> repositoryClass = permissionCheckingUtils.getRepositoryClass(proceedingJoinPoint);
        if (!repositoryClass.isPresent()) {
            throw new IllegalArgumentException("Unable to determine entity class!");
        }
        CrudRepository<?, ?> targetRepository = (CrudRepository<?, ?>) applicationContext.getBean(repositoryClass.get());
        if (!(targetRepository instanceof OrganizationResourceRepository)) {
            throw new IllegalArgumentException("Type of target repository should be OrganizationResourceRepository!");
        }
        Object targetId = proceedingJoinPoint.getArgs()[targetIdIndex];
        Optional<OrganizationAwareResource> targetOptional = ((OrganizationResourceRepository) targetRepository).findById((Serializable) targetId);
        if (!targetOptional.isPresent()) {
            throw new NotFoundException("Target not found");
        }
        permissionCheckingUtils.checkPermissionsByTarget(targetOptional.get(), user, resource, methodAnnotation.action());
        return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<CheckPermissionsByTargetId> supportedAnnotation() {
        return CheckPermissionsByTargetId.class;
    }
}
