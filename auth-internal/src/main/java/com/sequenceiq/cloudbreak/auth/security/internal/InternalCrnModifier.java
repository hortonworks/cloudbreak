package com.sequenceiq.cloudbreak.auth.security.internal;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

import javax.inject.Inject;

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

    public String getInternalCrnWithAccountId(String accountId) {
        return getAccountIdModifiedCrn(regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(), accountId);
    }

    public Object changeInternalCrn(ProceedingJoinPoint proceedingJoinPoint) {
        String userCrnString = ThreadBasedUserCrnProvider.getUserCrn();
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        if (userCrnString != null && RegionAwareInternalCrnGeneratorUtil.isInternalCrn(userCrnString)) {
            return ThreadBasedUserCrnProvider.doAs(getNewUserCrnIfAccountIdParamDefined(proceedingJoinPoint, methodSignature, userCrnString)
                        .or(() -> getNewUserCrnIfTenantAwareParamDefined(proceedingJoinPoint, methodSignature, userCrnString))
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

    private Optional<String> getNewUserCrnIfTenantAwareParamDefined(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature,
            String userCrnString) {
        Optional<Object> tenantAwareCrnOrObjectOptional = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, TenantAwareParam.class);
        if (tenantAwareCrnOrObjectOptional.isPresent()) {
            Object tenantAwareCrnOrObject = tenantAwareCrnOrObjectOptional.get();
            if (tenantAwareCrnOrObject instanceof String && Crn.isCrn((String) tenantAwareCrnOrObject)) {
                return getAccountModifiedUserCrnByTenantAwareCrnObject(userCrnString, tenantAwareCrnOrObject);
            } else if (Arrays.stream(tenantAwareCrnOrObject.getClass().getDeclaredFields()).
                    anyMatch(aField -> aField.isAnnotationPresent(TenantAwareParam.class))) {
                return getAccountModifiedUserCrnByTenantAwareField(userCrnString, tenantAwareCrnOrObject);
            }
        }
        return Optional.empty();
    }

    private Optional<String> getAccountModifiedUserCrnByTenantAwareField(String userCrnString, Object tenantAwareCrnOrObject) {
        try {
            Optional<Field> tenantAwareParamField = Arrays.stream(tenantAwareCrnOrObject.getClass().getDeclaredFields())
                    .filter(aField -> aField.isAnnotationPresent(TenantAwareParam.class))
                    .findFirst();
            Object tenantAwareParamFieldObject = PropertyUtils.getProperty(tenantAwareCrnOrObject, tenantAwareParamField.get().getName());
            if (tenantAwareParamFieldObject instanceof String && Crn.isCrn((String) tenantAwareParamFieldObject)) {
                return getAccountModifiedUserCrnByTenantAwareCrnObject(userCrnString, tenantAwareParamFieldObject);
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private Optional<String> getAccountModifiedUserCrnByTenantAwareCrnObject(String userCrnString, Object tenantAwareCrnObject) {
        String accountId = Crn.safeFromString((String) tenantAwareCrnObject).getAccountId();
        String newUserCrn = getAccountIdModifiedCrn(userCrnString, accountId);
        return Optional.of(newUserCrn);
    }

    private String getAccountIdModifiedCrn(String userCrnString, String accountId) {
        MDCBuilder.addTenant(accountId);
        Crn userCrn = Crn.fromString(userCrnString);
        Crn newUserCrn = Crn.copyWithDifferentAccountId(userCrn, accountId);
        LOGGER.debug("Changing internal CRN to {}", newUserCrn);
        createNewUser(newUserCrn);
        return newUserCrn.toString();
    }

    private void createNewUser(Crn newUserCrn) {
        CrnUser newUser = RegionAwareInternalCrnGeneratorUtil.createInternalCrnUser(newUserCrn);
        internalUserModifier.persistModifiedInternalUser(newUser);
    }
}
