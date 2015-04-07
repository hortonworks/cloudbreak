package com.sequenceiq.it;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.reporters.VerboseReporter;

class VerboseLogReporter extends VerboseReporter {
    private static final Logger LOG = LoggerFactory.getLogger(VerboseLogReporter.class);

    @Override
    protected void log(String message) {
        LOG.info(message.replaceAll("(?m)^", ""));
    }
}
