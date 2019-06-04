package com.sequenceiq.redbeams.exception;

import java.util.Objects;

public class RedbeamsException extends RuntimeException {

    public RedbeamsException(String message) {
        super(message);
    }

    public RedbeamsException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof RedbeamsException) {
            return getMessage().equals(((Throwable) o).getMessage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMessage());
    }
}
