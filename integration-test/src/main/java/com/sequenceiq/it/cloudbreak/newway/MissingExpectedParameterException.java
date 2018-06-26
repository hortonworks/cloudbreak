package com.sequenceiq.it.cloudbreak.newway;

class MissingExpectedParameterException extends RuntimeException {

    MissingExpectedParameterException(String key) {
        super(String.format("Missing test parameter value for key: '%s'", key));
    }
}
