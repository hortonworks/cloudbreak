package com.sequenceiq.cloudbreak.jerseyclient.retry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

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
            if (lastThrowable instanceof WebApplicationException) {
                WebApplicationException wae = (WebApplicationException) lastThrowable;
                if (wae.getResponse().getStatusInfo().getFamily() == Response.Status.Family.CLIENT_ERROR) {
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
        RetryContextSupport contextSupport = RetryContextSupport.class.cast(context);
        contextSupport.registerThrowable(throwable);
        if (throwable != null) {
            LOGGER.warn("Exception occurred during a REST API call.", throwable);
        }
    }
}
