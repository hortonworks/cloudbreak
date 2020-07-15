package com.sequenceiq.cloudbreak.auth.security.internal;

import java.util.Optional;

import javax.inject.Inject;

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

    public Object changeInternalCrn(ProceedingJoinPoint proceedingJoinPoint) {
        String userCrnString = ThreadBasedUserCrnProvider.getUserCrn();
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        if (userCrnString != null && InternalCrnBuilder.isInternalCrn(userCrnString)) {
            Optional<Object> tenantAwareCrn = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, TenantAwareParam.class);
            if (tenantAwareCrn.isPresent() && tenantAwareCrn.get() instanceof String
                    && Crn.isCrn((String) tenantAwareCrn.get())) {
                String accountId = Crn.fromString((String) tenantAwareCrn.get()).getAccountId();
                String newUserCrn = getAccountIdModifiedCrn(userCrnString, accountId);
                return ThreadBasedUserCrnProvider.doAs(newUserCrn.toString(),
                        () -> reflectionUtil.proceed(proceedingJoinPoint, methodSignature));
            }
            Optional<Object> accountId = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, AccountId.class);
            if (accountId.isPresent() && accountId.get() instanceof String) {
                String newUserCrn = getAccountIdModifiedCrn(userCrnString, (String) accountId.get());
                return ThreadBasedUserCrnProvider.doAs(newUserCrn.toString(),
                        () -> reflectionUtil.proceed(proceedingJoinPoint, methodSignature));
            }
        }
        return reflectionUtil.proceed(proceedingJoinPoint, methodSignature);
    }

    private String getAccountIdModifiedCrn(String userCrnString, String accountId) {
        Crn userCrn = Crn.fromString(userCrnString);
        Crn newUserCrn = Crn.builder()
                .setService(userCrn.getService())
                .setAccountId(accountId)
                .setResourceType(userCrn.getResourceType())
                .setResource(userCrn.getResource())
                .build();
        LOGGER.debug("Changing internal CRN to {}", newUserCrn);
        createNewUser(newUserCrn);
        return newUserCrn.toString();
    }

    public void createNewUser(Crn newUserCrn) {
        CrnUser newUser = InternalCrnBuilder.createInternalCrnUser(newUserCrn);
        internalUserModifier.persistModifiedInternalUser(newUser);
    }
}
