package com.sequenceiq.freeipa.kerberosmgmt.exception;

import java.util.Objects;

public class DeleteException extends RuntimeException {
    public DeleteException(String message) {
        super(message);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof DeleteException) {
            return getMessage().equals(((Throwable) o).getMessage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMessage());
    }
}
