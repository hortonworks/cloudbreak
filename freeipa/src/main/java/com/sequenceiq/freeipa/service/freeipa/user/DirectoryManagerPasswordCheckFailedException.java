package com.sequenceiq.freeipa.service.freeipa.user;

public class DirectoryManagerPasswordCheckFailedException extends Exception {

    public DirectoryManagerPasswordCheckFailedException(String message) {
        super(message);
    }

    public DirectoryManagerPasswordCheckFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
