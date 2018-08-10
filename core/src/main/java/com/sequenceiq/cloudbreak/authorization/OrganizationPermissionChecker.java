package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.lang.annotation.Annotation;

import javax.inject.Inject;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganization;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action;

@Component
public class OrganizationPermissionChecker implements PermissionChecker<CheckPermissionsByOrganization> {

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
        CheckPermissionsByOrganization methodAnnotation = (CheckPermissionsByOrganization) rawMethodAnnotation;
        int organizationIndex = methodAnnotation.organizationIndex();
        int length = proceedingJoinPoint.getArgs().length;
        validateOrgIndex(proceedingJoinPoint, organizationIndex, length);
        return ((Organization) proceedingJoinPoint.getArgs()[organizationIndex]).getId();
    }

    private void validateOrgIndex(JoinPoint proceedingJoinPoint, int organizationIndex, int length) {
        permissionCheckingUtils.validateIndex(organizationIndex, length, "organizationIndex");
        Object organization = proceedingJoinPoint.getArgs()[organizationIndex];
        if (!(organization instanceof Organization)) {
            throw new IllegalArgumentException(format("Type of organization should be %s!", Organization.class.getCanonicalName()));
        }
    }

    private <T extends Annotation> Action getAction(T rawMethodAnnotation) {
        return ((CheckPermissionsByOrganization) rawMethodAnnotation).action();
    }

    @Override
    public Class<CheckPermissionsByOrganization> supportedAnnotation() {
        return CheckPermissionsByOrganization.class;
    }
}
