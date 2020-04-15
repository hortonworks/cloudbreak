package com.sequenceiq.it.cloudbreak.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.reporters.VerboseReporter;

class VerboseLogReporter extends VerboseReporter {
    private static final Logger LOG = LoggerFactory.getLogger(VerboseLogReporter.class);

    VerboseLogReporter() {
        super("[TestNG] ");
    }

    @Override
    protected void log(String message) {
        LOG.info(message.replaceAll("(?m)^", ""));
    }
}
