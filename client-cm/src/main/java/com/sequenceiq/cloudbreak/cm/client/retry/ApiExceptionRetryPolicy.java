package com.sequenceiq.cloudbreak.cm.client.retry;

import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;

import com.cloudera.api.swagger.client.ApiException;

public class ApiExceptionRetryPolicy implements RetryPolicy {

    private static final int RETRY_LIMIT = 5;

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable lastThrowable = context.getLastThrowable();
        if (lastThrowable == null) {
            return true;
        }
        if (lastThrowable instanceof ApiException) {
            if (context.getRetryCount() < RETRY_LIMIT) {
                int code = ApiException.class.cast(lastThrowable).getCode();
                return code == HttpStatus.INTERNAL_SERVER_ERROR.value();
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
    }
}
