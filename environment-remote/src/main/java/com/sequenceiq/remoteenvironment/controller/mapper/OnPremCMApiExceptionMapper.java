package com.sequenceiq.remoteenvironment.controller.mapper;

import static jakarta.ws.rs.core.Response.Status.BAD_GATEWAY;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.remoteenvironment.exception.OnPremCMApiException;

@Provider
@Component
public class OnPremCMApiExceptionMapper extends BaseExceptionMapper<OnPremCMApiException> {
    private static final int BAD_GATEWAY_LIMIT = 400;

    @Override
    public Response.Status getResponseStatus(OnPremCMApiException exception) {
        return switch (exception.getStatusCode()) {
            case 0 -> SERVICE_UNAVAILABLE;
            case SC_UNAUTHORIZED, SC_FORBIDDEN -> BAD_GATEWAY;
            case SC_NOT_FOUND -> NOT_FOUND;
            case SC_INTERNAL_SERVER_ERROR -> INTERNAL_SERVER_ERROR;
            default -> exception.getStatusCode() >= BAD_GATEWAY_LIMIT ? BAD_GATEWAY : INTERNAL_SERVER_ERROR;
        };
    }

    @Override
    public Class<OnPremCMApiException> getExceptionType() {
        return OnPremCMApiException.class;
    }
}
