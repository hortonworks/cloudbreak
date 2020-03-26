package com.sequenceiq.freeipa.controller.exception;

import java.util.Objects;

public class UnsupportedException extends RuntimeException {
    public UnsupportedException(String message) {
        super(message);
    }

    public UnsupportedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof UnsupportedException) {
            return getMessage().equals(((Throwable) o).getMessage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMessage());
    }
}
