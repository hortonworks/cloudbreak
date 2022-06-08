package com.sequenceiq.authorization.service;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.util.Optional;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CustomPermissionCheck;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.ReflectionUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalUserModifier;
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

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) {
        long startTime = System.currentTimeMillis();
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        LOGGER.debug("Permission check started at {} (method: {})", startTime,
                methodSignature.getMethod().getDeclaringClass().getSimpleName() + '#' + methodSignature.getMethod().getName());
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        boolean internalUser = userCrn != null && RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrn);
        Optional<String> initiatorUserCrnParameter = persistInitiatorUserIfParameterPresent(proceedingJoinPoint, methodSignature);

        if (!internalUser && !hasAnnotationOnClass(proceedingJoinPoint, DisableCheckPermissions.class)
                && !hasAnyAnnotationOnMethod(methodSignature, DisableCheckPermissions.class, CustomPermissionCheck.class)) {
            checkNotNull(userCrn, "userCrn should not be null.");
            validateNotInternalOnly(proceedingJoinPoint, methodSignature);
            if (hasAnnotationOnMethod(methodSignature, CheckPermissionByAccount.class)) {
                accountAuthorizationService.authorize(methodSignature.getMethod().getAnnotation(CheckPermissionByAccount.class), userCrn);
            }
            resourceAuthorizationService.authorize(userCrn, proceedingJoinPoint, methodSignature);
        }

        if (internalUser && initiatorUserCrnParameter.isPresent()) {
            MDCBuilder.addTenant(Crn.fromString(initiatorUserCrnParameter.get()).getAccountId());
            return ThreadBasedUserCrnProvider.doAs(initiatorUserCrnParameter.get(), () ->
                    commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime));
        } else {
            if (regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString()
                    .equals(Crn.safeFromString(userCrn).getAccountId()) && accountIdNeeded(methodSignature)) {
                LOGGER.error("Method {} is not prepared to call internally, please check readme in authorization module.",
                        methodSignature.getMethod().getDeclaringClass().getSimpleName() + '#' + methodSignature.getMethod().getName());
                throw new AccessDeniedException("This API is not prepared to use it in service-to-service communication.");
            }
            return commonPermissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature, startTime);
        }
    }

    private boolean accountIdNeeded(MethodSignature methodSignature) {
        return !hasAnnotationOnMethod(methodSignature, AccountIdNotNeeded.class);
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
        return new AccessDeniedException("This API is not publicly available and therefore not usable by end users. " +
                "Please refer to our documentation about public APIs used by our UI and CLI.");
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
}
