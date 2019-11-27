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
import com.sequenceiq.cloudbreak.auth.security.CrnUser;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;

@Service
public class InternalCrnModifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(InternalCrnModifier.class);

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Inject
    private InternalUserModifier internalUserModifier;

    @Inject
    private ReflectionUtil reflectionUtil;

    public Object changeInternalCrn(ProceedingJoinPoint proceedingJoinPoint) {
        String userCrnString = threadBasedUserCrnProvider.getUserCrn();
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        if (userCrnString != null && InternalCrnBuilder.isInternalCrn(userCrnString)) {
            Optional<Object> resourceCrn = reflectionUtil.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class);
            if (resourceCrn.isPresent() && resourceCrn.get() instanceof String
                    && Crn.isCrn((String) resourceCrn.get())) {
                String accountId = Crn.fromString((String) resourceCrn.get()).getAccountId();
                Crn userCrn = Crn.fromString(userCrnString);
                Crn newUserCrn = Crn.builder()
                        .setService(userCrn.getService())
                        .setAccountId(accountId)
                        .setResourceType(userCrn.getResourceType())
                        .setResource(userCrn.getResource())
                        .build();
                LOGGER.debug("Changing internal CRN to {}", newUserCrn);
                threadBasedUserCrnProvider.removeUserCrn();
                threadBasedUserCrnProvider.setUserCrn(newUserCrn.toString());
                createNewUser(newUserCrn);
            }
        }
        return reflectionUtil.proceed(proceedingJoinPoint, methodSignature);
    }

    public void createNewUser(Crn newUserCrn) {
        CrnUser newUser = InternalCrnBuilder.createInternalCrnUser(newUserCrn);
        internalUserModifier.persistModifiedInternalUser(newUser);
    }
}
