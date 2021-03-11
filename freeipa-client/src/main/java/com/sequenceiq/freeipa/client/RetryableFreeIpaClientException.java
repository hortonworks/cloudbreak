package com.sequenceiq.freeipa.client;

import java.util.OptionalInt;

public class RetryableFreeIpaClientException extends FreeIpaClientException {

    public static final String MAX_RETRIES_EXPRESSION = "#{${freeipa.client.retry.retries}}";

    public static final String DELAY_EXPRESSION = "#{${freeipa.client.retry.delay}}";

    public static final String MULTIPLIER_EXPRESSION = "#{${freeipa.client.retry.multiplier}}";

    private final Exception exceptionForRestApi;

    public RetryableFreeIpaClientException(String message, Throwable cause) {
        super(message, cause);
        this.exceptionForRestApi = null;
    }

    public RetryableFreeIpaClientException(String message, Throwable cause, OptionalInt statusCode) {
        super(message, cause, statusCode);
        this.exceptionForRestApi = null;
    }

    public RetryableFreeIpaClientException(String message, RetryableFreeIpaClientException cause, Exception exceptionForRestApi) {
        super(message, cause, cause.getStatusCode());
        this.exceptionForRestApi = exceptionForRestApi;
    }

    public RetryableFreeIpaClientException(String message, RetryableFreeIpaClientException cause) {
        super(message, cause, cause.getStatusCode());
        exceptionForRestApi = cause.exceptionForRestApi;
    }

    public Exception getExceptionForRestApi() {
        return exceptionForRestApi;
    }

    @Override
    public String toString() {
        return super.toString() + System.lineSeparator() + "ExceptionForRestApi: "  + exceptionForRestApi.toString();
    }
}
