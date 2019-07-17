package com.sequenceiq.freeipa.controller.exception;

import java.util.Objects;

public class SyncOperationAlreadyRunningException extends RuntimeException {

    public SyncOperationAlreadyRunningException(String message) {
        super(message);
    }

    public SyncOperationAlreadyRunningException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof SyncOperationAlreadyRunningException) {
            return getMessage().equals(((Throwable) o).getMessage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMessage());
    }
}
