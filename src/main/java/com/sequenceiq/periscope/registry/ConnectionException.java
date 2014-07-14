package com.sequenceiq.periscope.registry;

public class ConnectionException extends Exception {
    public ConnectionException(String host) {
        super("Cannot connect to host: " + host);
    }
}
