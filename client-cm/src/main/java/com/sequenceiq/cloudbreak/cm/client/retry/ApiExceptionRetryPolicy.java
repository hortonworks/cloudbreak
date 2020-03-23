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
            return true;
        }
        if (lastThrowable instanceof ApiException) {
            if (context.getRetryCount() <= maxAttempts) {
                int code = ApiException.class.cast(lastThrowable).getCode();
                HttpStatus httpStatus = HttpStatus.valueOf(code);
                boolean httpStatus5xxServerError = httpStatus.is5xxServerError();
                LOGGER.warn("{} Exception occurred during CM API call, retryable: {} ({}/{})",
                        code,
                        httpStatus5xxServerError,
                        context.getRetryCount(),
                        maxAttempts);
                return httpStatus5xxServerError;
            } else {
                return false;
            }
        }
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
        }
    }
}
