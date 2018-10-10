package com.sequenceiq.cloudbreak.aspect;

import static java.lang.String.format;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.CrudRepositoryLookupService;

@Service
public class HasPermissionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HasPermissionService.class);

    @Inject
    private AuthorizationService authorizationService;

    @Inject
    private CrudRepositoryLookupService repositoryLookupService;

    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return proceedingJoinPoint.proceed();
        }

        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        DisableHasPermission disabled = getAnnotation(methodSignature, DisableHasPermission.class);
        if (disabled != null) {
            return proceedingJoinPoint.proceed();
        }

        HasPermission annotation = getAnnotation(methodSignature, HasPermission.class);

        validate(proceedingJoinPoint, annotation);

        if (annotation.condition() == ConditionType.PRE) {
            Object target = findTargetByAnnotation(proceedingJoinPoint, annotation);
            LOGGER.info("PRE-{} permission for: {}", annotation.permission(), target);
            authorizationService.hasPermission(target, annotation.permission().name());
        }

        Object proceed = proceedingJoinPoint.proceed();

        if (proceed == null) {
            logBecauseOfReturnNull(methodSignature);
        }

        if (annotation.condition() == ConditionType.POST) {
            LOGGER.info("POST-{} permission for: {}", annotation.permission(), proceed);
            authorizationService.hasPermission(proceed, annotation.permission().name());
        }

        return proceed;
    }

    private void logBecauseOfReturnNull(Signature signature) {
        LOGGER.info("Return value is null, method signature: {}", signature.toLongString());
    }

    private Object findTargetByIndex(JoinPoint proceedingJoinPoint, int index) {
        return needLookup(proceedingJoinPoint, index) ? lookup(proceedingJoinPoint, index) : getTargetByIndex(proceedingJoinPoint, index);
    }

    private Object lookup(JoinPoint proceedingJoinPoint, int index) {
        ParameterizedType type = getParameterizedType(proceedingJoinPoint);
        Class<?> resourceType = (Class<?>) type.getActualTypeArguments()[0];
        CrudRepository<?, Serializable> repository = repositoryLookupService.getRepositoryForEntity(resourceType);
        return repository.findById((Serializable) proceedingJoinPoint.getArgs()[index]).orElse(null);
    }

    private boolean needLookup(JoinPoint proceedingJoinPoint, int index) {
        ParameterizedType type = getParameterizedType(proceedingJoinPoint);
        Type idType = type.getActualTypeArguments()[1];
        Class<?> typeClass = proceedingJoinPoint.getArgs()[index].getClass();
        return typeClass.equals(idType);

    }

    protected ParameterizedType getParameterizedType(JoinPoint proceedingJoinPoint) {
        return (ParameterizedType) proceedingJoinPoint.getSignature().getDeclaringType().getGenericInterfaces()[0];
    }

    private <A extends Annotation> A getAnnotation(MethodSignature methodSignature, Class<A> annotationClass) {
        A annotation = methodSignature.getMethod().getAnnotation(annotationClass);
        if (annotation == null) {
            annotation = (A) methodSignature.getDeclaringType().getAnnotation(annotationClass);
        }
        return annotation;
    }

    private void validate(JoinPoint proceedingJoinPoint, HasPermission annotation) {
        if (annotation == null) {
            throw new IllegalArgumentException(format("HasPermission should be added to %s class.",
                    proceedingJoinPoint.getSignature().getDeclaringTypeName()));
        }

        if (annotation.permission() == PermissionType.WRITE && annotation.condition() != ConditionType.PRE) {
            throw new IllegalArgumentException("Write permission is enabled with Pre condition only.");
        }

        int length = proceedingJoinPoint.getArgs().length;
        if (length < annotation.targetIndex()) {
            throw new IllegalArgumentException(format("The argument count [%s] cannot be less than the target index[%s]", length, annotation.targetIndex()));
        }
    }

    private Object getTargetByIndex(JoinPoint proceedingJoinPoint, int index) {
        if (proceedingJoinPoint.getArgs() != null) {
            return proceedingJoinPoint.getArgs()[index];
        }
        return null;
    }

    private Object findTargetByAnnotation(JoinPoint proceedingJoinPoint, HasPermission annotation) {
        int targetIndex = annotation.targetIndex();
        Object ret = null;

        if (targetIndex > -1) {
            ret = findTargetByIndex(proceedingJoinPoint, targetIndex);
        }
        return ret;
    }
}
