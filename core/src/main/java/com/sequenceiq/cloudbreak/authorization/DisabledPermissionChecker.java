package com.sequenceiq.cloudbreak.authorization;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.organization.DisableCheckPermissions;
import com.sequenceiq.cloudbreak.domain.organization.User;

@Component
public class DisabledPermissionChecker implements PermissionChecker<DisableCheckPermissions> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, OrganizationResource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
    }

    @Override
    public Class<DisableCheckPermissions> supportedAnnotation() {
        return DisableCheckPermissions.class;
    }
}
