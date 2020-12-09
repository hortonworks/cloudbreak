package com.sequenceiq.authorization.service;

import com.sequenceiq.cloudbreak.auth.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

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
