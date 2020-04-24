package com.sequenceiq.it.cloudbreak.util.wait.service;

import java.util.Objects;

public class Result<R, E> {

    private final R result;

    private final E exception;

    private Result(R result, E exception) {
        this.result = result;
        this.exception = exception;
    }

    public static <R, E> Result<R, E> result(R result) {
        return new Result<>(Objects.requireNonNull(result), null);
    }

    public static <R, E> Result<R, E> exception(E exception) {
        return new Result<>(null, Objects.requireNonNull(exception));
    }

    public boolean isResult() {
        return result != null;
    }

    public boolean isException() {
        return exception != null;
    }

    public R getResult() {
        if (isException()) {
            throw new IllegalStateException("No result is present.");
        }
        return result;
    }

    public E getException() {
        if (isResult()) {
            throw new IllegalStateException("No exception is present.");
        }
        return exception;
    }
}
