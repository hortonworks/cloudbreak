package com.sequenceiq.cloudbreak.cloud.gcp.context;

/**
 * This exception would thrown if the/a created GcpContext instance has not
 * created properly, or some of it's field has filled with improper data.
 */
public class InvalidGcpContextException extends Exception {
    public InvalidGcpContextException(String message) {
        super(message);
    }
}
