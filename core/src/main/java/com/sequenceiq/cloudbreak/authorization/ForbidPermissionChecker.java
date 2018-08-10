package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.lang.annotation.Annotation;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.organization.ForbidForOrganizationResource;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Resource;

@Component
public class ForbidPermissionChecker implements PermissionChecker<ForbidForOrganizationResource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForbidPermissionChecker.class);

    @Override
    public <T extends Annotation> Object checkPermissions(T rawMethodAnnotation, Resource resource, User user, ProceedingJoinPoint proceedingJoinPoint,
            MethodSignature methodSignature) {
        LOGGER.error(String.format("Calling this method is forbidden. %s # %s", methodSignature.getDeclaringTypeName(), methodSignature.getMethod().getName()));
        throw new AccessDeniedException(format("You have no access to %s.", resource.getReadableName()));
    }

    @Override
    public Class<ForbidForOrganizationResource> supportedAnnotation() {
        return ForbidForOrganizationResource.class;
    }
}
