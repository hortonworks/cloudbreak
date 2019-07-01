package com.sequenceiq.freeipa.kerberosmgmt.exception;

import java.util.Objects;

public class KeytabCreationException extends RuntimeException {
    public KeytabCreationException(String message) {
        super(message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof KeytabCreationException) {
            return getMessage().equals(((Throwable) o).getMessage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMessage());
    }
}
