package com.sequenceiq.cloudbreak.authorization;

import java.lang.annotation.Annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Resource;

public interface PermissionChecker<T extends Annotation> {

    <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, Resource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature);

    Class<T> supportedAnnotation();
}
