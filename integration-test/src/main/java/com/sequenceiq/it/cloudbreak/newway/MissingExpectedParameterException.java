package com.sequenceiq.it.cloudbreak.newway;

import org.testng.SkipException;

public class MissingExpectedParameterException extends SkipException {

    public MissingExpectedParameterException(String key) {
        super(String.format("Missing test parameter value for key: '%s'", key));
    }
}
