package com.sequenceiq.cloudbreak.cloud.azure.util;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.StatusType;

import org.springframework.stereotype.Component;

import com.microsoft.aad.msal4j.MsalServiceException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class MsalServiceExceptionMapper extends BaseExceptionMapper<MsalServiceException> {

    @Override
    public StatusType getResponseStatus(MsalServiceException exception) {
        int statusCode = exception.statusCode() != null ? exception.statusCode() : -1;
        if (BAD_REQUEST.getStatusCode() == statusCode || UNAUTHORIZED.getStatusCode() == statusCode || FORBIDDEN.getStatusCode() == statusCode) {
            return BAD_REQUEST;
        }
        return INTERNAL_SERVER_ERROR;
    }

    @Override
    public Class<MsalServiceException> getExceptionType() {
        return MsalServiceException.class;
    }
}
