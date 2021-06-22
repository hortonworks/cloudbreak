package com.sequenceiq.authorization.service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.InternalCrnBuilder;

public class CustomCheckUtil {

    private CustomCheckUtil() {

    }

    public static void run(String actorCrn, Runnable runnable) {
        if (!InternalCrnBuilder.isInternalCrn(actorCrn)) {
            runnable.run();
        }
    }

    public static void run(Runnable runnable) {
        if (!InternalCrnBuilder.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
            runnable.run();
        }
    }
}
