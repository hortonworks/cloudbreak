package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.ResourceType;

@Component
public class CommonPermissionCheckingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonPermissionCheckingUtils.class);

    @Inject
    private UmsAuthorizationService umsAuthorizationService;

    public void checkPermissionForUser(ResourceType resource, ResourceAction action, String userCrn) {
        umsAuthorizationService.checkRightOfUserForResource(userCrn, resource, action);
    }

    public void checkPermissionForUser(ResourceType resource, ResourceAction action, String userCrn, String resourceCrn) {
        umsAuthorizationService.checkRightOfUserForResourceOnResource(userCrn, resource, action, resourceCrn);
    }

    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        try {
            Object proceed = proceedingJoinPoint.proceed();
            if (proceed == null) {
                LOGGER.debug("Return value is null, method signature: {}", methodSignature.toLongString());
            }
            return proceed;
        } catch (Error | RuntimeException unchecked) {
            throw unchecked;
        } catch (Throwable t) {
            throw new AccessDeniedException(t.getMessage(), t);
        }
    }

    Optional<Annotation> getClassAnnotation(Class<?> repositoryClass) {
        return Arrays.stream(repositoryClass.getAnnotations())
                .filter(a -> a.annotationType().equals(AuthorizationResource.class))
                .findFirst();
    }

    public Optional<Class<?>> getAuthorizationClass(ProceedingJoinPoint proceedingJoinPoint) {
        return proceedingJoinPoint.getTarget().getClass().isAnnotationPresent(AuthorizationResource.class)
                ? Optional.of(proceedingJoinPoint.getTarget().getClass()) : Optional.empty();
    }

}
