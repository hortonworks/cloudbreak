package com.sequenceiq.authorization.service;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;

@Component
public class CustomCheckUtil {

    public void run(String actorCrn, Runnable runnable) {
        if (!RegionAwareInternalCrnGeneratorUtil.isInternalCrn(actorCrn)) {
            runnable.run();
        }
    }

    public void run(Runnable runnable) {
        if (!RegionAwareInternalCrnGeneratorUtil.isInternalCrn(ThreadBasedUserCrnProvider.getUserCrn())) {
            runnable.run();
        }
    }
}
