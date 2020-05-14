package com.sequenceiq.cloudbreak.common.exception;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WebApplicationExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebApplicationExceptionHandler.class);

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public WebApplicationException handleException(WebApplicationException e) {
        String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
        LOGGER.warn("Handling WebApplicationException with message: " + errorMessage, e);
        if (e.getResponse().getStatusInfo().getFamily() == Family.CLIENT_ERROR) {
            throw new BadRequestException(errorMessage);
        }
        throw new InternalServerErrorException(errorMessage);
    }
}
