package com.sequenceiq.cloudbreak.service.exception;

public class RepositoryCannotFoundException extends RuntimeException {
    public RepositoryCannotFoundException(String message) {
        super(message);
    }

    public RepositoryCannotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
