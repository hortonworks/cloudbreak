package com.sequenceiq.cloudbreak.common.exception;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

@Component
public class WebApplicationExceptionHandler {

    public WebApplicationException handleException(WebApplicationException e) {
        String errorMessage = e.getMessage();
        if (e.getResponse().getStatusInfo().getFamily() == Response.Status.Family.CLIENT_ERROR) {
            throw new BadRequestException(errorMessage);
        }
        throw new InternalServerErrorException(errorMessage);
    }
}
