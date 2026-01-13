package com.sequenceiq.cloudbreak.jerseyclient.retry;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.context.RetryContextSupport;

public class JerseyClientRetryPolicy implements RetryPolicy {

    private static final Logger LOGGER = LoggerFactory.getLogger(JerseyClientRetryPolicy.class);

    private static final int RETRY_LIMIT = 5;

    @Override
    public boolean canRetry(RetryContext context) {
        Throwable lastThrowable = context.getLastThrowable();
        if (lastThrowable == null) {
            return true;
        } else {
            LOGGER.debug("Retry attempt {}. {}", context.getRetryCount(), context.getLastThrowable().toString());
            if (lastThrowable instanceof WebApplicationException wae) {
                Response.StatusType statusInfo = wae.getResponse().getStatusInfo();
                if (statusInfo.getFamily() == Response.Status.Family.CLIENT_ERROR) {
                    return false;
                } else if (statusInfo.getStatusCode() == Response.Status.BAD_REQUEST.getStatusCode()) {
                    return false;
                }
            }
        }
        return context.getRetryCount() <= RETRY_LIMIT;
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
        RetryContextSupport contextSupport = (RetryContextSupport) context;
        contextSupport.registerThrowable(throwable);
        if (throwable != null) {
            LOGGER.warn("Exception occurred during a REST API call.", throwable);
        }
    }
}
