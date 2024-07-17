package com.sequenceiq.cloudbreak.cloud.aws.common.exception;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.StatusType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

import software.amazon.awssdk.core.exception.SdkServiceException;

@Component
public class SdkServiceExceptionMapper extends BaseExceptionMapper<SdkServiceException> {

    @Override
    public StatusType getResponseStatus(SdkServiceException exception) {
        int statusCode = exception.statusCode();
        if (BAD_REQUEST.getStatusCode() == statusCode || UNAUTHORIZED.getStatusCode() == statusCode || FORBIDDEN.getStatusCode() == statusCode) {
            return BAD_REQUEST;
        }
        return INTERNAL_SERVER_ERROR;
    }

    @Override
    public Class<SdkServiceException> getExceptionType() {
        return SdkServiceException.class;
    }
}
