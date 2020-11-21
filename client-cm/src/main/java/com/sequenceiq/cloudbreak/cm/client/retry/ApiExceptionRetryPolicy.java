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
            LOGGER.debug("lastThrowable is null");
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
        HttpStatus httpStatus = HttpStatus.valueOf(code);
        boolean httpStatus5xxServerError = httpStatus.is5xxServerError();
        LOGGER.warn("{} Exception occurred during CM API call, retryable: {} ({}/{})", code, httpStatus5xxServerError, context.getRetryCount(), maxAttempts);
        return httpStatus5xxServerError;
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
