package com.sequenceiq.cloudbreak.auth.security.internal;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.NotImplementedException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.CrnUser;
import com.sequenceiq.cloudbreak.auth.InternalCrnBuilder;

@Service
public class InternalCrnModifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalCrnModifier.class);

    @Inject
    private InternalUserModifier internalUserModifier;

    @Inject
    private ReflectionUtil reflectionUtil;

    @Inject
    private Optional<AccountIdProvider> accountIdProvider;

    public Object changeInternalCrn(ProceedingJoinPoint proceedingJoinPoint) {
        String userCrnString = ThreadBasedUserCrnProvider.getUserCrn();
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        if (userCrnString != null && InternalCrnBuilder.isInternalCrn(userCrnString)) {
            String newUserCrn = changeUserCrnByCrnParamIfPossible(proceedingJoinPoint, userCrnString, methodSignature);
            newUserCrn = changeUserCrnByNameParamIfPossible(proceedingJoinPoint, newUserCrn, methodSignature);
            newUserCrn = changeUserCrnByApiModelParamIfPossible(proceedingJoinPoint, newUserCrn, methodSignature);
            return ThreadBasedUserCrnProvider.doAs(newUserCrn,
                    () -> reflectionUtil.proceed(proceedingJoinPoint, methodSignature));
        }
        return reflectionUtil.proceed(proceedingJoinPoint, methodSignature);
    }

    private String changeUserCrnByCrnParamIfPossible(ProceedingJoinPoint proceedingJoinPoint, String originalUserCrn, MethodSignature methodSignature) {
        Optional<Object> resourceCrn = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, TenantAwareCrnParam.class);
        if (resourceCrn.isPresent() && resourceCrn.get() instanceof String
                && Crn.isCrn((String) resourceCrn.get())) {
            String accountId = Crn.fromString((String) resourceCrn.get()).getAccountId();
            return getNewUserCrn(originalUserCrn, accountId);
        }
        return originalUserCrn;
    }

    private String changeUserCrnByNameParamIfPossible(ProceedingJoinPoint proceedingJoinPoint, String originalUserCrn, MethodSignature methodSignature) {
        try {
            Optional<Object> resourceName = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, TenantAwareNameParam.class);
            if (resourceName.isPresent() && resourceName.get() instanceof String) {
                String accountId = accountIdProvider.orElseThrow(() -> new NotImplementedException("AccountIdProvider is not implemented in this mocrservice!"))
                        .getAccountIdByResourceName((String) resourceName.get());
                return getNewUserCrn(originalUserCrn, accountId);
            }
        } catch (Exception e) {
            LOGGER.error("Cannot determine account id for internal actor crn change, falling back to default.", e);
        }
        return originalUserCrn;
    }

    private String changeUserCrnByApiModelParamIfPossible(ProceedingJoinPoint proceedingJoinPoint, String originalUserCrn, MethodSignature methodSignature) {
        Optional<Object> apiModel = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, TenantAwareApiModel.class);
        if (apiModel.isPresent() && apiModel.get() instanceof TenantAwareApiModel) {
            String accountId = ((TenantAwareApiModel) apiModel.get()).getAccountId();
            return getNewUserCrn(originalUserCrn, accountId);
        }
        return originalUserCrn;
    }

    private String getNewUserCrn(String originalUserCrn, String accountId) {
        Crn userCrn = Crn.fromString(originalUserCrn);
        Crn newUserCrn = generateCrnWithNewAccountId(accountId, userCrn);
        LOGGER.debug("Changing internal CRN to {}", newUserCrn);
        createNewUser(newUserCrn);
        return newUserCrn.toString();
    }

    private Crn generateCrnWithNewAccountId(String accountId, Crn userCrn) {
        return Crn.builder()
                .setService(userCrn.getService())
                .setAccountId(accountId)
                .setResourceType(userCrn.getResourceType())
                .setResource(userCrn.getResource())
                .build();
    }

    public void createNewUser(Crn newUserCrn) {
        CrnUser newUser = InternalCrnBuilder.createInternalCrnUser(newUserCrn);
        internalUserModifier.persistModifiedInternalUser(newUser);
    }
}
