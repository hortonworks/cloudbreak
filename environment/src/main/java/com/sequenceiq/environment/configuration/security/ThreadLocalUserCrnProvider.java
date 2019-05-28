package com.sequenceiq.environment.configuration.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

@Service
public class ThreadLocalUserCrnProvider {

    private static final ThreadLocal<String> USER_CRN = new ThreadLocal<>();

    public String getUserCrn() {
        return USER_CRN.get();
    }

    public String getAccountId() {
        String userCrn = getUserCrn();
        if (userCrn != null) {
            Crn crn = Crn.fromString(userCrn);
            return crn.getAccountId();
        } else {
            throw new AccessDeniedException("CRN is not set!");
        }
    }

    void setUserCrn(String userCrn) {
        USER_CRN.set(userCrn);
    }

    void removeUserCrn() {
        USER_CRN.remove();
    }
}
