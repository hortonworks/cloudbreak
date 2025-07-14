package com.sequenceiq.cloudbreak.auth.security.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.ReflectionUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;

@Service
public class InternalCrnModifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalCrnModifier.class);

    @Inject
    private InternalUserModifier internalUserModifier;

    @Inject
    private ReflectionUtil reflectionUtil;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Object changeInternalCrn(ProceedingJoinPoint proceedingJoinPoint) {
        String userCrnString = ThreadBasedUserCrnProvider.getUserCrn();
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        if (userCrnString != null && RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrnString)) {
            return ThreadBasedUserCrnProvider.doAs(getNewUserCrnIfAccountIdParamDefined(proceedingJoinPoint, methodSignature, userCrnString)
                        .or(() -> getNewUserCrnIfResourceCrnParamDefined(proceedingJoinPoint, methodSignature, userCrnString))
                        .orElse(userCrnString),
                    () -> reflectionUtil.proceed(proceedingJoinPoint));
        }
        return reflectionUtil.proceed(proceedingJoinPoint);
    }

    private Optional<String> getNewUserCrnIfAccountIdParamDefined(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature,
            String userCrnString) {
        Optional<Object> accountId = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, AccountId.class);
        if (accountId.isPresent() && accountId.get() instanceof String) {
            String newUserCrn = getAccountIdModifiedCrn(userCrnString, (String) accountId.get());
            return Optional.of(newUserCrn);
        }
        return Optional.empty();
    }

    private Optional<String> getNewUserCrnIfResourceCrnParamDefined(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature,
            String userCrnString) {
        Optional<Object> resourceCrnObjectOptional = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class);
        if (resourceCrnObjectOptional.isPresent()) {
            Object resourceCrnObject = resourceCrnObjectOptional.get();
            if (resourceCrnObject instanceof String && Crn.isCrn((String) resourceCrnObject)) {
                return getAccountModifiedUserCrnByResourceCrnObject(userCrnString, resourceCrnObject);
            }
        }
        Optional<Object> requestObjectOptional = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, RequestObject.class);
        if (requestObjectOptional.isPresent()) {
            Object requestObject = requestObjectOptional.get();
            if (Arrays.stream(requestObject.getClass().getDeclaredFields()).anyMatch(aField -> aField.isAnnotationPresent(ResourceCrn.class)) ||
                    Arrays.stream(requestObject.getClass().getMethods()).anyMatch(aMethod -> aMethod.isAnnotationPresent(ResourceCrn.class))) {
                return getAccountModifiedUserCrnByResourceCrnField(userCrnString, requestObject);
            }
        }
        return Optional.empty();
    }

    private Optional<String> getAccountModifiedUserCrnByResourceCrnField(String userCrnString, Object requestObject) {
        try {
            Optional<Field> resourceCrnField = Arrays.stream(requestObject.getClass().getDeclaredFields())
                    .filter(aField -> aField.isAnnotationPresent(ResourceCrn.class))
                    .findFirst();
            if (resourceCrnField.isPresent()) {
                Object resourceCrnFieldObject = PropertyUtils.getProperty(requestObject, resourceCrnField.get().getName());
                if (resourceCrnFieldObject instanceof String && Crn.isCrn((String) resourceCrnFieldObject)) {
                    return getAccountModifiedUserCrnByResourceCrnObject(userCrnString, resourceCrnFieldObject);
                }
            }
            Optional<Method> resourceCrnMethod = Arrays.stream(requestObject.getClass().getMethods())
                    .filter(aMethod -> aMethod.isAnnotationPresent(ResourceCrn.class))
                    .findFirst();
            if (resourceCrnMethod.isPresent()) {
                Object resourceCrnMethodObject = MethodUtils.invokeMethod(requestObject, resourceCrnMethod.get().getName(), null);
                if (resourceCrnMethodObject instanceof String && Crn.isCrn((String) resourceCrnMethodObject)) {
                    return getAccountModifiedUserCrnByResourceCrnObject(userCrnString, resourceCrnMethodObject);
                }
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<String> getAccountModifiedUserCrnByResourceCrnObject(String userCrnString, Object resourceCrnObject) {
        String accountId = Crn.safeFromString((String) resourceCrnObject).getAccountId();
        String newUserCrn = getAccountIdModifiedCrn(userCrnString, accountId);
        return Optional.of(newUserCrn);
    }

    private Crn changeAccountIdInCrnString(String userCrnString, String accountId) {
        return Crn.copyWithDifferentAccountId(Crn.safeFromString(userCrnString), accountId);
    }

    private String getAccountIdModifiedCrn(String userCrnString, String accountId) {
        MDCBuilder.addTenant(accountId);
        Crn newUserCrn = changeAccountIdInCrnString(userCrnString, accountId);
        LOGGER.trace("Changing internal CRN to {}", newUserCrn);
        createNewUser(newUserCrn);
        return newUserCrn.toString();
    }

    private void createNewUser(Crn newUserCrn) {
        CrnUser newUser = RegionAwareInternalCrnGeneratorUtil.createInternalCrnUser(newUserCrn);
        internalUserModifier.persistModifiedInternalUser(newUser);
    }
}
