package com.sequenceiq.cloudbreak.authorization;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action;

@Component
public class ReturnValuePermissionChecker implements PermissionChecker<CheckPermissionsByReturnValue> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, OrganizationResource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionsByReturnValue methodAnnotation = (CheckPermissionsByReturnValue) rawMethodAnnotation;
        Action action = methodAnnotation.action();
        Object proceed = permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        if (proceed instanceof Optional<?>) {
            Optional<?> optionalResult = (Optional<?>) proceed;
            if (!optionalResult.isPresent()) {
                return proceed;
            }
            permissionCheckingUtils.checkPermissionsByTarget(optionalResult.get(), user, resource, action);
        } else {
            permissionCheckingUtils.checkPermissionsByTarget(proceed, user, resource, action);
        }
        return proceed;
    }

    @Override
    public Class<CheckPermissionsByReturnValue> supportedAnnotation() {
        return CheckPermissionsByReturnValue.class;
    }
}
