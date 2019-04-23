package com.sequenceiq.datalake.util;

import org.springframework.stereotype.Service;

@Service
public class RestRequestThreadLocalService {

    private static final ThreadLocal<String> USER_CRN = new ThreadLocal<>();

    public void setUserCrn(String userCrn) {
        USER_CRN.set(userCrn);
    }

    public String getUserCrn() {
        return USER_CRN.get();
    }

    public void removeUserCrn() {
        USER_CRN.remove();
    }

}
