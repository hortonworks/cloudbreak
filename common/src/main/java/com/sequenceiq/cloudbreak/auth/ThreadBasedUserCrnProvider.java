package com.sequenceiq.cloudbreak.auth;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.altus.Crn;

public class ThreadBasedUserCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadBasedUserCrnProvider.class);

    private static final ThreadLocal<String> USER_CRN = new ThreadLocal<>();

    private ThreadBasedUserCrnProvider() {
    }

    @Nullable
    public static String getUserCrn() {
        return USER_CRN.get();
    }

    public static String getAccountId() {
        String userCrn = getUserCrn();
        if (userCrn != null) {
            return Optional.ofNullable(Crn.fromString(userCrn)).orElseThrow(() -> new IllegalStateException("Unable to obtain crn!")).getAccountId();
        } else {
            throw new IllegalStateException("Crn is not set!");
        }
    }

    public static void setUserCrn(String userCrn) {
        if (USER_CRN.get() != null) {
            String errorMessage = String.format("Trying to set crn %s when it already contains %s, please check where we didn't remove it!",
                    userCrn, USER_CRN.get());
            String stackTrace = Arrays.stream(Thread.currentThread().getStackTrace())
                    .map(StackTraceElement::toString).collect(Collectors.joining("\n", "\n", "\n"));
            LOGGER.error(errorMessage + " Stack trace on thread: " + stackTrace);
            throw new IllegalStateException(errorMessage);
        } else {
            USER_CRN.set(userCrn);
        }
    }

    public static void removeUserCrn() {
        USER_CRN.remove();
    }
}
