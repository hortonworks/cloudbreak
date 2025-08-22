package com.sequenceiq.cloudbreak.common.exception;

import java.util.Objects;

public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof BadRequestException) {
            return getMessage().equals(((Throwable) o).getMessage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMessage());
    }
}
