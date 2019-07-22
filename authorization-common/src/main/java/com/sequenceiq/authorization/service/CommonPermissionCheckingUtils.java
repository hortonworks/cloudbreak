package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;

@Component
public class CommonPermissionCheckingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonPermissionCheckingUtils.class);

    @Inject
    private UmsAuthorizationService umsWorkspaceAuthorizationService;

    public void checkPermissionForUser(AuthorizationResource resource, ResourceAction action, String userCrn) {
        umsWorkspaceAuthorizationService.checkRightOfUserForResource(userCrn, resource, action);
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        try {
            Object proceed = proceedingJoinPoint.proceed();
            if (proceed == null) {
                LOGGER.debug("Return value is null, method signature: {}", methodSignature.toLongString());
            }
            return proceed;
        } catch (Throwable t) {
            throw new AccessDeniedException(t.getMessage(), t);
        }
    }

    Optional<Annotation> getClassAnnotation(Class<?> repositoryClass) {
        return Arrays.stream(repositoryClass.getAnnotations())
                .filter(a -> a.annotationType().equals(AuthorizationResourceType.class))
                .findFirst();
    }

    public Optional<Class<?>> getRepositoryClass(ProceedingJoinPoint proceedingJoinPoint, List<Class> repositoryClass) {
        return Arrays.stream(proceedingJoinPoint.getTarget().getClass().getInterfaces())
                .filter(i -> {
                    List<Class<?>> interfaces = Arrays.asList(i.getInterfaces());
                    return repositoryClass.stream().filter(repoClass -> interfaces.contains(repoClass)).findAny().isPresent();
                })
                .findFirst();
    }

}
