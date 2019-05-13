package com.sequenceiq.cloudbreak.authorization;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByTargetId;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceResourceRepository;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

@Component
public class TargetIdPermissionChecker implements PermissionChecker<CheckPermissionsByTargetId> {

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, WorkspaceResource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionsByTargetId methodAnnotation = (CheckPermissionsByTargetId) rawMethodAnnotation;
        int targetIdIndex = methodAnnotation.targetIdIndex();
        int length = proceedingJoinPoint.getArgs().length;
        permissionCheckingUtils.validateIndex(targetIdIndex, length, "targetIdIndex");
        Optional<Class<?>> repositoryClass = permissionCheckingUtils.getWorkspaceAwareRepositoryClass(proceedingJoinPoint);
        if (!repositoryClass.isPresent()) {
            throw new IllegalArgumentException("Unable to determine entity class!");
        }
        CrudRepository<?, Serializable> targetRepository = (CrudRepository<?, Serializable>) applicationContext.getBean(repositoryClass.get());
        if (!(targetRepository instanceof WorkspaceResourceRepository)) {
            throw new IllegalArgumentException("Type of target repository should be WorkspaceResourceRepository!");
        }
        Serializable targetId = (Serializable) proceedingJoinPoint.getArgs()[targetIdIndex];
        Optional<WorkspaceAwareResource> targetOptional = ((DisabledBaseRepository<WorkspaceAwareResource, Serializable>) targetRepository).findById(targetId);
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
