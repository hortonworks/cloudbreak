package com.sequenceiq.it.cloudbreak.action;

import javax.ws.rs.NotAuthorizedException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.context.TestContext;

public class UmsTimeoutWorkaroundUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsTimeoutWorkaroundUtils.class);

    private static final String APP_LEVEL_UMS_TIMEOUT_MESSAGE = "Authorization failed due to user management service call timed out.";

    private UmsTimeoutWorkaroundUtils() {

    }

    public static final boolean shouldRetry(TestContext testContext, Exception e) {
        return testContext.isUmsWorkaroundRetryEnabled() &&
                testContext.umsUserCacheInUse() &&
                umsTimeoutRelatedException(e);
    }

    public static final boolean shouldRetry(TestContext testContext, Exception e, boolean additionalConditionForApplicationError) {
        return testContext.isUmsWorkaroundRetryEnabled() &&
                testContext.umsUserCacheInUse() &&
                (umsTimeoutOnAuthenticationLevel(e) || (umsTimeoutOnApplicationLevel(e) && additionalConditionForApplicationError));
    }

    public static boolean umsTimeoutRelatedException(Exception e) {
        return umsTimeoutOnAuthenticationLevel(e) || umsTimeoutOnApplicationLevel(e);
    }

    public static boolean umsTimeoutOnAuthenticationLevel(Exception e) {
        // unfortunately there is no way to tell if 401 is actually an authentication error or an ums timeout
        // since cdp gateway returns with 401 anyway
        // but worth to retry once in case of 401
        return e instanceof NotAuthorizedException || e.getCause() instanceof NotAuthorizedException;
    }

    public static boolean umsTimeoutOnApplicationLevel(Exception e) {
        return e.getMessage().contains(APP_LEVEL_UMS_TIMEOUT_MESSAGE) ||
                (e.getCause() != null && e.getCause().getMessage().contains(APP_LEVEL_UMS_TIMEOUT_MESSAGE));
    }

    public static void waitForUmsIfNeeded(TestContext testContext, Exception ex) {
        if (shouldRetry(testContext, ex)) {
            waitForUms();
        }
    }

    public static void waitForUms() {
        int backoff = 5000;
        LOGGER.info("UMS timeout occurred, flow polling is backed of for {} ms", backoff);
        try {
            Thread.sleep(backoff);
        } catch (InterruptedException e) {
            LOGGER.error("UMS backoff is interrupted: ", e);
        }
    }
}
