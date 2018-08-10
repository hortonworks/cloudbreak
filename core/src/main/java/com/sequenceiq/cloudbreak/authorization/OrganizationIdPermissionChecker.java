package com.sequenceiq.cloudbreak.authorization;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action;

@Component
public class OrganizationIdPermissionChecker implements PermissionChecker<CheckPermissionsByOrganizationId> {

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, OrganizationResource resource, User user,
            ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Long orgId = getOrgId(rawMethodAnnotation, proceedingJoinPoint);
        Action action = getAction(rawMethodAnnotation);
        return permissionCheckingUtils.checkPermissionsByPermissionSetAndProceed(resource, user, orgId, action, proceedingJoinPoint, methodSignature);
    }

    private <T extends Annotation> Long getOrgId(T rawMethodAnnotation, ProceedingJoinPoint proceedingJoinPoint) {
        CheckPermissionsByOrganizationId methodAnnotation = (CheckPermissionsByOrganizationId) rawMethodAnnotation;
        int organizationIdIndex = methodAnnotation.organizationIdIndex();
        int length = proceedingJoinPoint.getArgs().length;
        validateOrgIdIndex(proceedingJoinPoint, organizationIdIndex, length);
        return (Long) proceedingJoinPoint.getArgs()[organizationIdIndex];
    }

    private void validateOrgIdIndex(JoinPoint proceedingJoinPoint, int organizationIdIndex, int length) {
        permissionCheckingUtils.validateIndex(organizationIdIndex, length, "organizationIdIndex");
        Object orgId = proceedingJoinPoint.getArgs()[organizationIdIndex];
        if (!(orgId instanceof Long)) {
            throw new IllegalArgumentException("Type of organizationId should be Long!");
        }
    }

    private <T extends Annotation> Action getAction(T rawMethodAnnotation) {
        return ((CheckPermissionsByOrganizationId) rawMethodAnnotation).action();
    }

    @Override
    public Class<CheckPermissionsByOrganizationId> supportedAnnotation() {
        return CheckPermissionsByOrganizationId.class;
    }
}
