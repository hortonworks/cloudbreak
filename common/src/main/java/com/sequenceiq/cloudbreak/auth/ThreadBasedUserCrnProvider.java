package com.sequenceiq.cloudbreak.auth;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

@Service
public class ThreadBasedUserCrnProvider {

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
            throw new IllegalStateException("Crn is not set!");
        }
    }

    public void setUserCrn(String userCrn) {
        USER_CRN.set(userCrn);
    }

    public void removeUserCrn() {
        USER_CRN.remove();
    }
}
