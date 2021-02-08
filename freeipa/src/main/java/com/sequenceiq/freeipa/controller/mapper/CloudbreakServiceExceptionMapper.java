package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class CloudbreakServiceExceptionMapper extends BaseExceptionMapper<CloudbreakServiceException> {
    @Override
    Response.Status getResponseStatus(CloudbreakServiceException exception) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    Class<CloudbreakServiceException> getExceptionType() {
        return CloudbreakServiceException.class;
    }
}
