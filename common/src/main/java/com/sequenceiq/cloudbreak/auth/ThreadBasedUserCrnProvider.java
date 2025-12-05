package com.sequenceiq.cloudbreak.auth;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.app.StaticApplicationContext;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.Crn.Service;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorUtil;
import com.sequenceiq.cloudbreak.logger.MdcContext;

public class ThreadBasedUserCrnProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadBasedUserCrnProvider.class);

    private static final InheritableThreadLocal<String> USER_CRN = new InheritableThreadLocal<>();

    private static final String DEFAULT_REGION = "us-west-1";

    private static final String DEFAULT_PARTITION = "cdp";

    private static final String REGION_KEY = "crn.region";

    private static final String PARTITION_KEY = "crn.partition";

    private static String region;

    private static String partition;

    private ThreadBasedUserCrnProvider() {
    }

    @Nullable
    public static String getUserCrn() {
        return USER_CRN.get();
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
            addUserCrnAndTenantToMdcContext(userCrn);
        }
    }

    public static String getAccountId() {
        String userCrn = getUserCrn();
        if (userCrn != null) {
            return Optional.ofNullable(Crn.fromString(userCrn)).orElseThrow(() -> new IllegalStateException("Unable to obtain crn!")).getAccountId();
        } else {
            throw new IllegalStateException("Crn is not set!");
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

    public static <T, W extends Throwable> T doAsAndThrow(String userCrn, ThrowableCallable<T, W> callable) throws W {
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

    public static <T> T doAsCallable(String userCrn, Callable<T> callable) throws Exception {
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

    public static <T> T doAsInternalActor(Supplier<T> callable) {
        String originalUserCrn = getUserCrn();
        String accountId = getAccountIdIfAvailable(originalUserCrn);
        if (StringUtils.isEmpty(accountId) || RegionAwareInternalCrnGeneratorUtil.INTERNAL_ACCOUNT.equals(accountId)) {
            LOGGER.warn("Missing or invalid accountId [{}] for internal actor. Operation may execute with incorrect authorization.", accountId,
                    new IllegalArgumentException());
        }
        String internalCrn = getInternalUserCrn(accountId);
        return doAs(internalCrn, callable);
    }

    public static <T> T doAsInternalActor(Supplier<T> callable, String accountId) {
        if (StringUtils.isEmpty(accountId) || RegionAwareInternalCrnGeneratorUtil.INTERNAL_ACCOUNT.equals(accountId)) {
            LOGGER.warn("Missing or invalid accountId [{}] for internal actor. Operation may execute with incorrect authorization.", accountId,
                    new IllegalArgumentException());
        }
        String internalCrn = getInternalUserCrn(accountId);
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

    public static void doAsInternalActor(Runnable runnable) {
        String originalUserCrn = getUserCrn();
        String accountId = getAccountIdIfAvailable(originalUserCrn);
        if (StringUtils.isEmpty(accountId) || RegionAwareInternalCrnGeneratorUtil.INTERNAL_ACCOUNT.equals(accountId)) {
            LOGGER.warn("Missing or invalid accountId [{}] for internal actor. Operation may execute with incorrect authorization.", accountId,
                    new IllegalArgumentException());
        }
        String internalCrn = getInternalUserCrn(accountId);
        doAs(internalCrn, runnable);
    }

    public static <E extends Throwable> void doAsInternalActor(ThrowableRunnable<E> runnable, String accountId) throws E {
        String internalCrn = getInternalUserCrn(accountId);
        doAsAndThrow(internalCrn, runnable);
    }

    public static <E extends Throwable> void doAsAndThrow(String userCrn, ThrowableRunnable<E> runnable) throws E {
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

    public static <T> T doAsInternalActor(Function<String, T> originalUserCrnConsumer) {
        String originalUserCrn = getUserCrn();
        String accountId = getAccountIdIfAvailable(originalUserCrn);
        if (StringUtils.isEmpty(accountId) || RegionAwareInternalCrnGeneratorUtil.INTERNAL_ACCOUNT.equals(accountId)) {
            LOGGER.warn("Missing or invalid accountId [{}] for internal actor. Operation may execute with incorrect authorization.", accountId,
                    new IllegalArgumentException());
        }
        String internalCrn = getInternalUserCrn(accountId);
        return doAs(internalCrn, () -> originalUserCrnConsumer.apply(originalUserCrn));
    }

    private static String getAccountIdIfAvailable(String originalUserCrn) {
        if (originalUserCrn == null) {
            return null;
        }
        Crn originalUser = Crn.fromString(originalUserCrn);
        if (originalUser == null) {
            return null;
        }
        return originalUser.getAccountId();
    }

    private static String getInternalUserCrn(String accountId) {
        loadRegionAwareConfiguration();
        if (accountId == null) {
            return RegionAwareInternalCrnGenerator.regionalAwareInternalCrnGenerator(Service.IAM, partition, region).getInternalCrnForServiceAsString();
        } else {
            return RegionAwareInternalCrnGenerator.regionalAwareInternalCrnGenerator(Service.IAM, partition, region, accountId)
                    .getInternalCrnForServiceAsString();
        }
    }

    private static void loadRegionAwareConfiguration() {
        if (region == null) {
            region = StaticApplicationContext.getProperty(REGION_KEY, DEFAULT_REGION);
        }
        if (partition == null) {
            partition = StaticApplicationContext.getProperty(PARTITION_KEY, DEFAULT_PARTITION);
        }
    }

    private static void addUserCrnAndTenantToMdcContext(String userCrn) {
        MdcContext.Builder builder = MdcContext.builder();
        doIfNotNull(userCrn, builder::userCrn);
        builder.buildMdc();
    }
}
