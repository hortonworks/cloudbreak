package com.sequenceiq.cloudbreak.blueprint.filesystem;

public class FileSystemConfigException extends RuntimeException {

    public FileSystemConfigException(String message) {
        super(message);
    }

    public FileSystemConfigException(String message, Throwable cause) {
        super(message, cause);
    }

}
