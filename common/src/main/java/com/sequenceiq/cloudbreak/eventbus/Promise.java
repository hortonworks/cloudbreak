package com.sequenceiq.cloudbreak.eventbus;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class Promise<T> {

    @JsonIgnore
    private final CompletableFuture<T> completableFuture = new CompletableFuture<>();

    public static <T> Promise<T> prepare() {
        return new Promise<>();
    }

    public void accept(T value) {
        if (!isComplete()) {
            completableFuture.complete(value);
        }
    }

    public T await(long timeout, TimeUnit timeUnit) throws InterruptedException {
        try {
            return completableFuture.get(timeout, timeUnit);
        } catch (TimeoutException e) {
            throw new CloudbreakServiceException(e);
        } catch (ExecutionException e) {
            if (e.getCause() != null && e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new CloudbreakServiceException(e);
        }
    }

    public T await() throws InterruptedException {
        try {
            return completableFuture.get();
        } catch (ExecutionException e) {
            throw new CloudbreakServiceException(e);
        }
    }

    public boolean isComplete() {
        return completableFuture.isDone();
    }

    public void onNext(T value) {
        if (!isComplete()) {
            completableFuture.complete(value);
        }
    }

    public void onError(Object cause) {
        if (!isComplete()) {
            if (cause instanceof Throwable) {
                completableFuture.completeExceptionally((Throwable) cause);
            } else {
                completableFuture.completeExceptionally(new CloudbreakServiceException(cause.toString()));
            }
        }
    }
}
