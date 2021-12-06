package com.sequenceiq.cloudbreak.cluster.exception;

public class ClusterManagerCheckedException extends Exception {

    // TODO CB-14929: Extend this with some kind of enums / codes to indicate types of errors.
    //  - Example: Connectivity issues.

    public ClusterManagerCheckedException(String message) {
        super(message);
    }

    public ClusterManagerCheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterManagerCheckedException(Throwable cause) {
        super(cause);
    }
}
