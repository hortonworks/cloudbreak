package com.sequenceiq.cloudbreak.common.exception;

import java.util.Objects;

public class UpgradeValidationFailedException extends RuntimeException {

    public UpgradeValidationFailedException(String message) {
        super(message);
    }

    public UpgradeValidationFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UpgradeValidationFailedException(Throwable cause) {
        super(cause);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof UpgradeValidationFailedException) {
            return getMessage().equals(((Throwable) o).getMessage());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getMessage());
    }
}
