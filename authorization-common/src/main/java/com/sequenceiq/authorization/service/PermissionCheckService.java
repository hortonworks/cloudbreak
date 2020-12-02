package com.sequenceiq.authorization.service;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.service.list.ListPermissionChecker;
import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.ReflectionUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalUserModifier;
import com.sequenceiq.cloudbreak.logger.LoggerContextKey;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class PermissionCheckService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckService.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private ListPermissionChecker listPermissionChecker;

    @Inject
    private InternalUserModifier internalUserModifier;

    @Inject
    private CrnUserDetailsService crnUserDetailsService;

    @Inject
    private ReflectionUtil reflectionUtil;

    @Inject
    private AccountAuthorizationService accountAuthorizationService;

    @Inject
    private ResourceAuthorizationService resourceAuthorizationService;

    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) {
        long startTime = System.currentTimeMillis();
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        LOGGER.debug("Permission check started at {} (method: {})", startTime,
                methodSignature.getMethod().getDeclaringClass().getSimpleName() + '#' + methodSignature.getMethod().getName());
        Optional<Object> initiatorUserCrn = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, InitiatorUserCrn.class);
        if (InternalCrnBuilder.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())
                && initiatorUserCrn.isPresent()
                && initiatorUserCrn.get() instanceof String && Crn.isCrn((String) initiatorUserCrn.get())) {
            String newUserCrn = (String) initiatorUserCrn.get();
            internalUserModifier.persistModifiedInternalUser(crnUserDetailsService.loadUserByUsername(newUserCrn));
            return ThreadBasedUserCrnProvider.doAs(newUserCrn, () -> commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime));
        }

        if (commonPermissionCheckingUtils.isAuthorizationDisabled(proceedingJoinPoint)
                || hasAnyAnnotation(methodSignature, DisableCheckPermissions.class, CustomPermissionCheck.class)
                || InternalCrnBuilder.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
            return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
        }

        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        validateNotInternalOnly(proceedingJoinPoint, methodSignature);

        if (hasAnnotation(methodSignature, CheckPermissionByAccount.class)) {
            accountAuthorizationService.authorize(methodSignature.getMethod().getAnnotation(CheckPermissionByAccount.class), userCrn);
        }

        if (hasAnnotation(methodSignature, FilterListBasedOnPermissions.class)) {
            FilterListBasedOnPermissions listFilterAnnotation = methodSignature.getMethod().getAnnotation(FilterListBasedOnPermissions.class);
            return listPermissionChecker.checkPermissions(listFilterAnnotation, userCrn, proceedingJoinPoint, methodSignature, startTime);
        }

        resourceAuthorizationService.authorize(userCrn, proceedingJoinPoint, methodSignature, getRequestId());
        return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
    }

    private void validateNotInternalOnly(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        if (commonPermissionCheckingUtils.isInternalOnly(proceedingJoinPoint)) {
            throw getAccessDeniedAndLogInternalActorRestriction(methodSignature);
        }
        if (hasAnnotation(methodSignature, InternalOnly.class)) {
            throw getAccessDeniedAndLogInternalActorRestriction(methodSignature);
        }
    }

    private AccessDeniedException getAccessDeniedAndLogInternalActorRestriction(MethodSignature methodSignature) {
        LOGGER.error("Method {} should be called by internal actor only.",
                methodSignature.getMethod().getDeclaringClass().getSimpleName() + '#' + methodSignature.getMethod().getName());
        return new AccessDeniedException("You have no access to this resource.");
    }

    @SafeVarargs
    private boolean hasAnyAnnotation(MethodSignature methodSignature, Class<? extends Annotation>... annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (hasAnnotation(methodSignature, annotation)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnnotation(MethodSignature methodSignature, Class<? extends Annotation> annotation) {
        return methodSignature.getMethod().isAnnotationPresent(annotation);
    }

    protected Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }
}
