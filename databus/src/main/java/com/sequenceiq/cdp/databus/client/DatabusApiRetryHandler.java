package com.sequenceiq.cdp.databus.client;

import java.time.Duration;

import javax.ws.rs.core.Response.Status;

import com.cloudera.cdp.CdpClientException;
import com.cloudera.cdp.CdpHTTPException;
import com.cloudera.cdp.CdpServiceException;
import com.cloudera.cdp.http.RetryHandler;

/**
 * Retry handler for databus client, it only do retries if the HTTP response is 504.
 */
public class DatabusApiRetryHandler implements RetryHandler {

    private static final String UNAVAILABLE_STATUS_MSG = "UNAVAILABLE";

    private static final int DEFAULT_ATTEMPTS = 3;

    private static final int DEFAULT_DELAY_IN_SEC = 5;

    private final int attempts;

    private final int delayInSeconds;

    private final boolean retryOnServerUnavailable;

    private DatabusApiRetryHandler(Builder builder) {
        this.attempts = builder.attempts;
        this.delayInSeconds = builder.delayInSeconds;
        this.retryOnServerUnavailable = builder.retryOnServerUnavailable;
    }

    @Override
    public Duration shouldRetry(int attempts, CdpClientException exception) {
        CdpHTTPException httpEx = null;
        if (exception instanceof CdpServiceException) {
            httpEx = (CdpServiceException) exception;
        } else if (exception instanceof CdpHTTPException) {
            httpEx = (CdpHTTPException) exception;
        }
        if (retryOnServerUnavailable && httpEx != null) {
            boolean serviceUnavailable = isServiceUnavailable(httpEx);
            if (serviceUnavailable && attempts <= this.attempts) {
                return Duration.ofSeconds(delayInSeconds);
            }
        }
        return RetryHandler.DO_NOT_RETRY;
    }

    private boolean isServiceUnavailable(CdpHTTPException httpEx) {
        if (Status.INTERNAL_SERVER_ERROR.equals(Status.fromStatusCode(httpEx.getHttpCode())) && httpEx instanceof CdpServiceException
                && ((CdpServiceException) httpEx).getStatusMessage().startsWith(UNAVAILABLE_STATUS_MSG)) {
            return true;
        } else {
            return Status.SERVICE_UNAVAILABLE.equals(Status.fromStatusCode(httpEx.getHttpCode()));
        }
    }

    public static class Builder {

        private int attempts = DEFAULT_ATTEMPTS;

        private int delayInSeconds = DEFAULT_DELAY_IN_SEC;

        private boolean retryOnServerUnavailable;

        private Builder() {
        }

        public static Builder builder() {
            return new Builder();
        }

        public DatabusApiRetryHandler build() {
            return new DatabusApiRetryHandler(this);
        }

        public Builder withAttempts(int attempts) {
            this.attempts = attempts;
            return this;
        }

        public Builder withDelayInSeconds(int delayInSeconds) {
            this.delayInSeconds = delayInSeconds;
            return this;
        }

        public Builder withRetryOnServerUnavailable(boolean retryOnServerUnavailable) {
            this.retryOnServerUnavailable = retryOnServerUnavailable;
            return this;
        }
    }
}
