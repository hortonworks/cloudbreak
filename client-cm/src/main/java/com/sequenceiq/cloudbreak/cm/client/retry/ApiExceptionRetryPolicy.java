package com.sequenceiq.cloudbreak.cm.client.retry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;

import com.cloudera.api.swagger.client.ApiException;

public class ApiExceptionRetryPolicy implements RetryPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionRetryPolicy.class);

    private static final String REGEX_5XX = "5\\d{2}";

    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private volatile int maxAttempts;

    public ApiExceptionRetryPolicy() {
        this(DEFAULT_MAX_ATTEMPTS);
    }

    public ApiExceptionRetryPolicy(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable lastThrowable = context.getLastThrowable();
        if (lastThrowable == null) {
            return true;
        } else {
            return handleLastThrowableNotNull(context, lastThrowable);
        }
    }

    private boolean handleLastThrowableNotNull(RetryContext context, Throwable lastThrowable) {
        if (lastThrowable instanceof ApiException) {
            return handleApiException(context, (ApiException) lastThrowable);
        } else {
            return handleNotApiException(lastThrowable);
        }
    }

    private boolean handleApiException(RetryContext context, ApiException lastThrowable) {
        if (context.getRetryCount() <= maxAttempts) {
            return handleMaxRetryCountNotReached(context, lastThrowable);
        } else {
            LOGGER.debug("Max attempt reached: attempt: [{}] of [{}]", context.getRetryCount(), maxAttempts);
            return false;
        }
    }

    private boolean handleMaxRetryCountNotReached(RetryContext context, ApiException lastThrowable) {
        int code = lastThrowable.getCode();
        if (code != 0) {
            return handleStatusCodeNotZero(context, code);
        } else {
            LOGGER.debug("HTTP status code is 0");
            return true;
        }
    }

    private boolean handleStatusCodeNotZero(RetryContext context, int code) {
        boolean httpStatus5xxServerError = isHttpStatus5xxServerError(code);
        // retry for 401 is needed because of OPSAPS-68536
        boolean retryable = httpStatus5xxServerError || code == HttpStatus.UNAUTHORIZED.value();
        LOGGER.warn("{} Exception occurred during CM API call, retryable: {} ({}/{})", code, retryable, context.getRetryCount(), maxAttempts);
        return retryable;
    }

    private boolean isHttpStatus5xxServerError(int code) {
        try {
            HttpStatus httpStatus = HttpStatus.valueOf(code);
            return httpStatus.is5xxServerError();
        } catch (IllegalArgumentException e) {
            LOGGER.error(String.format("HTTP response status cannot be recognized, possibly it is a custom HTTP status (not specified in any RFCs): %d", code));
            return String.valueOf(code).matches(REGEX_5XX);
        }
    }

    private boolean handleNotApiException(Throwable lastThrowable) {
        LOGGER.debug("'lastThrowble' is not an 'ApiException'. Exception type: [{}]", lastThrowable.getClass().getCanonicalName());
        return false;
    }

    @Override
    public RetryContext open(RetryContext parent) {
        return new RetryContextSupport(parent);
    }

    @Override
    public void close(RetryContext context) {
    }

    @Override
    public void registerThrowable(RetryContext context, Throwable throwable) {
        RetryContextSupport contextSupport = RetryContextSupport.class.cast(context);
        contextSupport.registerThrowable(throwable);
        if (throwable != null) {
            LOGGER.warn("Exception occurred during CM API call.", throwable);
        } else {
            LOGGER.debug("Throwable is null");
        }
    }
}
