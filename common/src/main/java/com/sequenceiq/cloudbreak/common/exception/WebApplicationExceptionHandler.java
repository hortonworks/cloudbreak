package com.sequenceiq.cloudbreak.common.exception;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
public class WebApplicationExceptionHandler {

    @Inject
    private WebApplicationExceptionMessageExtractor webApplicationExceptionMessageExtractor;

    public WebApplicationException handleException(WebApplicationException e) {
        String errorMessage = webApplicationExceptionMessageExtractor.getErrorMessage(e);
        if (e.getResponse().getStatusInfo().getFamily() == Response.Status.Family.CLIENT_ERROR) {
            throw new BadRequestException(errorMessage);
        }
        throw new InternalServerErrorException(errorMessage);
    }
}
