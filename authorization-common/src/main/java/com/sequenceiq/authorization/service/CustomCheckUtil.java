package com.sequenceiq.authorization.service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;

public class CustomCheckUtil {

    private CustomCheckUtil() {

    }

    public static void run(String actorCrn, Runnable runnable) {
        if (!RegionAwareInternalCrnGeneratorUtil.isInternalCrn(actorCrn)) {
            runnable.run();
        }
    }

    public static void run(Runnable runnable) {
        if (!RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
            runnable.run();
        }
    }
}
