package com.sequenceiq.cloudbreak.auth;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;

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

    private static void setUserCrn(String userCrn) {
        if (StringUtils.isNotEmpty(USER_CRN.get())) {
            String errorMessage = String.format("Trying to set crn %s when it already contains %s!", userCrn, USER_CRN.get());
            String stackTrace = Arrays.stream(Thread.currentThread().getStackTrace())
                    .map(StackTraceElement::toString).collect(Collectors.joining("\n", "\n", "\n"));
            LOGGER.error(errorMessage + " Stack trace on thread: " + stackTrace);
            throw new IllegalStateException(errorMessage);
        } else {
            USER_CRN.set(userCrn);
        }
    }

    private static void removeUserCrn() {
        USER_CRN.remove();
    }

    public static void doAsForServlet(String userCrn, ServletRunnable runnable) throws ServletException, IOException {
        removeUserCrn();
        setUserCrn(userCrn);
        try {
            runnable.run();
        } finally {
            removeUserCrn();
        }
    }

    // CHECKSTYLE:OFF
    public static <T, W extends Throwable> T doAsAndThrow(String userCrn, ThrowableCallable<T, W> callable) throws Throwable {
        // CHECKSTYLE:ON
        String previousUserCrn = getUserCrn();
        removeUserCrn();
        setUserCrn(userCrn);
        try {
            return callable.call();
        } finally {
            removeUserCrn();
            if (previousUserCrn != null) {
                setUserCrn(previousUserCrn);
            }
        }
    }

    public static <T> T doAs(String userCrn, Supplier<T> callable) {
        String previousUserCrn = getUserCrn();
        removeUserCrn();
        setUserCrn(userCrn);
        try {
            return callable.get();
        } finally {
            removeUserCrn();
            if (previousUserCrn != null) {
                setUserCrn(previousUserCrn);
            }
        }
    }

    public static <T> T doAsInternalActor(String internalCrn, Supplier<T> callable) {
        String originalUserCrn = getUserCrn();
        if (originalUserCrn != null && RegionAwareInternalCrnGeneratorUtil.isInternalCrn(originalUserCrn)) {
            return doAs(originalUserCrn, callable);
        }
        return doAs(internalCrn, callable);
    }

    public static void doAs(String userCrn, Runnable runnable) {
        String previousUserCrn = getUserCrn();
        removeUserCrn();
        setUserCrn(userCrn);
        try {
            runnable.run();
        } finally {
            removeUserCrn();
            if (previousUserCrn != null) {
                setUserCrn(previousUserCrn);
            }
        }
    }

    public static void doAsInternalActor(String internalCrn, Runnable runnable) {
        String originalUserCrn = getUserCrn();
        if (originalUserCrn != null && RegionAwareInternalCrnGeneratorUtil.isInternalCrn(originalUserCrn)) {
            doAs(originalUserCrn, runnable);
        } else {
            doAs(internalCrn, runnable);
        }
    }
}
