package com.sequenceiq.cloudbreak.authorization;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsInPostPhase;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Action;
import com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Resource;

@Component
public class PostPhasePermissionChecker implements PermissionChecker<CheckPermissionsInPostPhase> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, Resource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {

        CheckPermissionsInPostPhase methodAnnotation = (CheckPermissionsInPostPhase) rawMethodAnnotation;
        Action action = methodAnnotation.action();
        Object proceed = permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        permissionCheckingUtils.checkPermissionsByTarget(proceed, user, resource, action);
        return proceed;
    }

    @Override
    public Class<CheckPermissionsInPostPhase> supportedAnnotation() {
        return CheckPermissionsInPostPhase.class;
    }
}
