package com.sequenceiq.cloudbreak.authorization;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.resource.ResourceAction;
import com.sequenceiq.cloudbreak.workspace.resource.WorkspaceResource;

@Component
public class ReturnValuePermissionChecker implements PermissionChecker<CheckPermissionsByReturnValue> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, WorkspaceResource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        CheckPermissionsByReturnValue methodAnnotation = (CheckPermissionsByReturnValue) rawMethodAnnotation;
        ResourceAction action = methodAnnotation.action();
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
