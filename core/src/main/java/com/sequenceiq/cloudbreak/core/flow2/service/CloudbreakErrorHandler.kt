package com.sequenceiq.cloudbreak.core.flow2.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import reactor.fn.Consumer

/**
 * Generic error handler for the Cloudbreak application.
 * Errors (Exceptions) need to be delegated to the error translator  / interpreter subsystem.
 *
 *
 * The main functionality of the subsystem is to provide meaningful error messages to the calling systems.
 */
@Component
class CloudbreakErrorHandler : Consumer<Throwable> {

    override fun accept(errorData: Throwable) {
        LOGGER.debug("Applying event specific error logic on error with message: {} ", errorData.message)
        errorLogic(errorData)
    }

    /**
     * Place for default event specific error consumption logic. ()

     * @param errorData the exception to be consumed
     */
    protected fun errorLogic(errorData: Throwable) {
        LOGGER.error("Default event specific error logic - logging the received throwable: ", errorData)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CloudbreakErrorHandler::class.java)
    }

}
