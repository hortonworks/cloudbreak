package com.sequenceiq.cloudbreak.cloud.azure.util;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.StatusType;

import org.springframework.stereotype.Component;

import com.azure.core.management.exception.ManagementException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class ManagementExceptionMapper extends BaseExceptionMapper<ManagementException> {

    @Override
    public StatusType getResponseStatus(ManagementException exception) {
        int statusCode = getStatusCode(exception);
        if (BAD_REQUEST.getStatusCode() == statusCode || UNAUTHORIZED.getStatusCode() == statusCode || FORBIDDEN.getStatusCode() == statusCode) {
            return BAD_REQUEST;
        }
        return INTERNAL_SERVER_ERROR;
    }

    private int getStatusCode(ManagementException exception) {
        return exception.getResponse() != null ? exception.getResponse().getStatusCode() : -1;
    }

    @Override
    public Class<ManagementException> getExceptionType() {
        return ManagementException.class;
    }
}
