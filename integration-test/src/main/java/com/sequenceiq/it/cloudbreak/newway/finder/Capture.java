package com.sequenceiq.it.cloudbreak.newway.finder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Capture {

    private static final Logger LOGGER = LoggerFactory.getLogger(Capture.class);

    private Object value;

    public Capture(Object value) {
        this.value = value;
    }

    public void verify(Object newValue) {
        LOGGER.info("verify the expected value {} with the actual one {}", value, newValue);

        if (!newValue.equals(value)) {
            throw new RuntimeException("Assertion failed, actual value:" + newValue + ", expected value:" + value);
        }
    }
}
