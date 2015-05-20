package com.sequenceiq.cloudbreak.core.flow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.function.Consumer;

/**
 * Generic error handler for the Cloudbreak application.
 * Errors (Exceptions) need to be delegated to the error translator  / interpreter subsystem.
 * <p/>
 * The main functionality of the subsystem is to provide meaningful error messages to the calling systems.
 */
@Component
public class CloudbreakErrorHandler implements Consumer<Throwable> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakErrorHandler.class);

    @Override
    public void accept(Throwable errorData) {
        LOGGER.debug("Consuming error:", errorData.getMessage());
        errorLogic(errorData);
    }

    /**
     * Place for default error consumption logic. ()
     *
     * @param errorData the exception to be consumed
     */
    protected void errorLogic(Throwable errorData) {
        LOGGER.info("Default error consumption logic: Do nothing. Exception message: {}", errorData.getMessage());
    }

}
