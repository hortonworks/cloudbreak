package com.sequenceiq.freeipa.service.freeipa.user;

public class DirectoryManagerPasswordUpdateFailedException extends Exception {

    public DirectoryManagerPasswordUpdateFailedException(String message) {
        super(message);
    }

    public DirectoryManagerPasswordUpdateFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
