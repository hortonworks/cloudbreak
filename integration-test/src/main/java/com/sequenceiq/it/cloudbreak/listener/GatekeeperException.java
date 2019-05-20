package com.sequenceiq.it.cloudbreak.listener;

import org.testng.SkipException;

public class GatekeeperException extends SkipException {
    public GatekeeperException(String skipMessage) {
        super(skipMessage);
    }

    public GatekeeperException(String skipMessage, Throwable cause) {
        super(skipMessage, cause);
    }
}
