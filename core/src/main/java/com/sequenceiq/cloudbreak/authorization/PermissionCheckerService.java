package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganization;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByTarget;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsInPostPhase;
import com.sequenceiq.cloudbreak.aspect.organization.ForbidForOrganizationResource;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.repository.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.validation.OrganizationPermissions.Resource;

@Service
public class PermissionCheckerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckerService.class);

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private UserService userService;

    @Inject
    private List<PermissionChecker<? extends Annotation>> permissionCheckers;

    @Inject
    private PermissionCheckingUtils permissionCheckingUtils;

    private final Map<Class<? extends Annotation>, PermissionChecker<? extends Annotation>> permissionCheckerMap = new HashMap<>();

    @PostConstruct
    public void populatePermissionCheckerMap() {
        permissionCheckers.forEach(permissionChecker
                -> permissionCheckerMap.put(permissionChecker.supportedAnnotation(), permissionChecker));
    }

    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) {
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);
        }
        IdentityUser cbUser = authenticatedUserService.getCbUser();
        User user = userService.getOrCreate(cbUser);
        return checkPermission(proceedingJoinPoint, methodSignature, user);
    }

    private Object checkPermission(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, User user) {
        Optional<Class<?>> repositoryClass = Arrays.stream(proceedingJoinPoint.getTarget().getClass().getInterfaces())
                .filter(i -> Arrays.asList(i.getInterfaces()).contains(OrganizationResourceRepository.class))
                .findFirst();

        Optional<Annotation> classAnnotation = repositoryClass.flatMap(repo -> Arrays.stream(repo.getAnnotations())
                .filter(a -> a.annotationType().equals(OrganizationResourceType.class))
                .findFirst());

        if (!repositoryClass.isPresent()) {
            return permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature);

        } else if (!classAnnotation.isPresent()) {
            throw denyAccessAndLogMissingAnnotation(repositoryClass.get());

        } else {
            OrganizationResourceType classOrgResourceType = (OrganizationResourceType) classAnnotation.get();

            if (classOrgResourceType.resource() == Resource.ALL) {
                throw denyAccessAndLogMissingAnnotation(repositoryClass.get());
            }

            CheckPermissionsByOrganization checkPermissionsByOrganization = methodSignature.getMethod()
                    .getAnnotation(CheckPermissionsByOrganization.class);
            CheckPermissionsByOrganizationId checkPermissionsByOrganizationId = methodSignature.getMethod()
                    .getAnnotation(CheckPermissionsByOrganizationId.class);
            CheckPermissionsByTarget checkPermissionsByTarget = methodSignature.getMethod()
                    .getAnnotation(CheckPermissionsByTarget.class);
            CheckPermissionsInPostPhase checkPermissionsInPostPhase = methodSignature.getMethod()
                    .getAnnotation(CheckPermissionsInPostPhase.class);
            ForbidForOrganizationResource forbidForOrganizationResource = methodSignature.getMethod()
                    .getAnnotation(ForbidForOrganizationResource.class);

            Annotation methodAnnotation = validateNumberOfAnnotations(methodSignature, checkPermissionsByOrganization, checkPermissionsByOrganizationId,
                    checkPermissionsByTarget, checkPermissionsInPostPhase, forbidForOrganizationResource);

            PermissionChecker<? extends Annotation> permissionChecker = permissionCheckerMap.get(methodAnnotation.annotationType());

            if (permissionChecker == null) {
                throw new IllegalStateException(format("Annotation %s is not supported.", methodAnnotation.annotationType().getCanonicalName()));
            }

            Resource resource = classOrgResourceType.resource();
            return permissionChecker.checkPermissions(methodAnnotation, resource, user, proceedingJoinPoint, methodSignature);
        }
    }

    private Annotation validateNumberOfAnnotations(MethodSignature methodSignature, CheckPermissionsByOrganization checkPermissionsByOrganization,
            CheckPermissionsByOrganizationId checkPermissionsByOrganizationId, CheckPermissionsByTarget checkPermissionsByTarget,
            CheckPermissionsInPostPhase checkPermissionsInPostPhase, ForbidForOrganizationResource forbidForOrganizationResource) {

        List<Annotation> annotations = Stream.of(checkPermissionsByOrganization,
                checkPermissionsByOrganizationId,
                checkPermissionsByTarget,
                checkPermissionsInPostPhase,
                forbidForOrganizationResource)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (annotations.isEmpty()) {
            throw new IllegalStateException(format("Method must be annotated: %s # %s",
                    methodSignature.getDeclaringTypeName(), methodSignature.getMethod().getName()));
        }

        if (annotations.size() > 1) {
            throw new IllegalStateException("Only one of these annotations can be added to method: checkPermissionsByOrganization,\n"
                    + "checkPermissionsByOrganizationId,\n"
                    + "checkPermissionsByTarget,\n"
                    + "checkPermissionsByTargetId,\n"
                    + "checkPermissionsInPostPhase,\n"
                    + "forbidForOrganizationResource");
        }

        return annotations.get(0);
    }

    private AccessDeniedException denyAccessAndLogMissingAnnotation(Class<?> repositoryClass) {
        LOGGER.error("Class '{}' should be annotated with @{} and specify the resource!",
                repositoryClass.getCanonicalName(), OrganizationResourceType.class.getName());
        return new AccessDeniedException("You have no access to this resource.");
    }
}
