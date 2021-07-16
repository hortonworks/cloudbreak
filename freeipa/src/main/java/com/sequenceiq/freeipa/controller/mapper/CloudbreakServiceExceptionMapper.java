package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class CloudbreakServiceExceptionMapper extends BaseExceptionMapper<CloudbreakServiceException> {
    @Override
    public Response.Status getResponseStatus(CloudbreakServiceException exception) {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    public Class<CloudbreakServiceException> getExceptionType() {
        return CloudbreakServiceException.class;
    }
}
