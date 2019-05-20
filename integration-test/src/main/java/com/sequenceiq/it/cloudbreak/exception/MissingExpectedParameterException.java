package com.sequenceiq.it.cloudbreak.exception;

import org.testng.SkipException;

public class MissingExpectedParameterException extends SkipException {

    public MissingExpectedParameterException(String key) {
        super(String.format("Missing test parameter value for key: '%s'", key));
    }
}
