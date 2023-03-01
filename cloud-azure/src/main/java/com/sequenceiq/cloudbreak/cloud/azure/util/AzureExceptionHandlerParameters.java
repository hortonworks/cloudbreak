package com.sequenceiq.cloudbreak.cloud.azure.util;

public class AzureExceptionHandlerParameters {

    private final boolean handleNotFound;

    private final boolean handleAllExceptions;

    public AzureExceptionHandlerParameters(boolean handleNotFound, boolean handleAllExceptions) {
        this.handleNotFound = handleNotFound;
        this.handleAllExceptions = handleAllExceptions;
    }

    public boolean isHandleNotFound() {
        return handleNotFound;
    }

    public boolean isHandleAllExceptions() {
        return handleAllExceptions;
    }

    @Override
    public String toString() {
        return "AzureExceptionHandlerParameters{" +
                "handleNotFound=" + handleNotFound +
                ", handleAllExceptions=" + handleAllExceptions +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AzureExceptionHandlerParameters that = (AzureExceptionHandlerParameters) o;

        if (isHandleNotFound() != that.isHandleNotFound()) {
            return false;
        }
        return isHandleAllExceptions() == that.isHandleAllExceptions();
    }

    @Override
    public int hashCode() {
        int result = (isHandleNotFound() ? 1 : 0);
        result = 31 * result + (isHandleAllExceptions() ? 1 : 0);
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean handleNotFound;

        private boolean handleAllExceptions;

        public Builder withHandleNotFound(boolean handleNotFound) {
            this.handleNotFound = handleNotFound;
            return this;
        }

        public Builder withHandleAllExceptions(boolean handleAllExceptions) {
            this.handleAllExceptions = handleAllExceptions;
            return this;
        }

        public AzureExceptionHandlerParameters build() {
            return new AzureExceptionHandlerParameters(handleNotFound, handleAllExceptions);
        }
    }
}
