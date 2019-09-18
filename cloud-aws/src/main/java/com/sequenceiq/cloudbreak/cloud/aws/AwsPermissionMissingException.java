package com.sequenceiq.cloudbreak.cloud.aws;

public class AwsPermissionMissingException extends Exception {

    public AwsPermissionMissingException() {
    }

    public AwsPermissionMissingException(String message) {
        super(message);
    }

}
