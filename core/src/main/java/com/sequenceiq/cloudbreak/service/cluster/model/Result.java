package com.sequenceiq.cloudbreak.service.cluster.model;

import java.util.Objects;

public class Result<S, E> {

    private final S success;

    private final E error;

    private Result(S success, E error) {
        this.success = success;
        this.error = error;
    }

    public static <S, E> Result<S, E> success(S success) {
        return new Result<>(Objects.requireNonNull(success), null);
    }

    public static <S, E> Result<S, E> error(E error) {
        return new Result<>(null, Objects.requireNonNull(error));
    }

    public boolean isSuccess() {
        return success != null;
    }

    public boolean isError() {
        return error != null;
    }

    public S getSuccess() {
        if (isError()) {
            throw new IllegalStateException("No successful value is present.");
        }
        return success;
    }

    public E getError() {
        if (isSuccess()) {
            throw new IllegalStateException("No error value is present.");
        }
        return error;
    }
}
