package com.sequenceiq.it

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.reporters.VerboseReporter

internal class VerboseLogReporter : VerboseReporter() {

    override fun log(message: String) {
        LOG.info(message.replace("(?m)^".toRegex(), ""))
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(VerboseLogReporter::class.java)
    }
}
