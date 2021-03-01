package com.sequenceiq.authorization.service;

import static com.google.common.base.Preconditions.checkNotNull;

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
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.ReflectionUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;
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
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        boolean internalUser = userCrn != null && InternalCrnBuilder.isInternalCrn(userCrn);
        Optional<String> initiatorUserCrnParameter = persistInitiatorUserIfParameterPresent(proceedingJoinPoint, methodSignature);

        if (!internalUser && !hasAnnotationOnClass(proceedingJoinPoint, DisableCheckPermissions.class)
                && !hasAnyAnnotationOnMethod(methodSignature, DisableCheckPermissions.class, CustomPermissionCheck.class)) {
            checkNotNull(userCrn);
            validateNotInternalOnly(proceedingJoinPoint, methodSignature);
            if (hasAnnotationOnMethod(methodSignature, CheckPermissionByAccount.class)) {
                accountAuthorizationService.authorize(methodSignature.getMethod().getAnnotation(CheckPermissionByAccount.class), userCrn);
            }
            resourceAuthorizationService.authorize(userCrn, proceedingJoinPoint, methodSignature, getRequestId());
        }

        if (internalUser && initiatorUserCrnParameter.isPresent()) {
            return ThreadBasedUserCrnProvider.doAs(initiatorUserCrnParameter.get(), () ->
                    commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime));
        } else {
            return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
        }
    }

    private Optional<String> persistInitiatorUserIfParameterPresent(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Optional<Object> initiatorUserCrn = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, InitiatorUserCrn.class);
        if (initiatorUserCrn.isPresent() && initiatorUserCrn.get() instanceof String && Crn.isCrn((String) initiatorUserCrn.get())) {
            String newUserCrn = (String) initiatorUserCrn.get();
            internalUserModifier.persistModifiedInternalUser(crnUserDetailsService.loadUserByUsername(newUserCrn));
            return Optional.of(newUserCrn);
        }
        return Optional.empty();
    }

    private void validateNotInternalOnly(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        if (hasAnnotationOnClass(proceedingJoinPoint, InternalOnly.class) || hasAnnotationOnMethod(methodSignature, InternalOnly.class)) {
            throw getAccessDeniedAndLogInternalActorRestriction(methodSignature);
        }
    }

    private AccessDeniedException getAccessDeniedAndLogInternalActorRestriction(MethodSignature methodSignature) {
        LOGGER.error("Method {} should be called by internal actor only.",
                methodSignature.getMethod().getDeclaringClass().getSimpleName() + '#' + methodSignature.getMethod().getName());
        return new AccessDeniedException("You have no access to this resource.");
    }

    @SafeVarargs
    private boolean hasAnyAnnotationOnMethod(MethodSignature methodSignature, Class<? extends Annotation>... annotations) {
        for (Class<? extends Annotation> annotation : annotations) {
            if (hasAnnotationOnMethod(methodSignature, annotation)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnnotationOnMethod(MethodSignature methodSignature, Class<? extends Annotation> annotation) {
        return methodSignature.getMethod().isAnnotationPresent(annotation);
    }

    private boolean hasAnnotationOnClass(ProceedingJoinPoint proceedingJoinPoint, Class<? extends Annotation> annotation) {
        return proceedingJoinPoint.getTarget().getClass().isAnnotationPresent(annotation);
    }

    protected Optional<String> getRequestId() {
        String requestId = MDCBuilder.getMdcContextMap().get(LoggerContextKey.REQUEST_ID.toString());
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        return Optional.of(requestId);
    }
}
