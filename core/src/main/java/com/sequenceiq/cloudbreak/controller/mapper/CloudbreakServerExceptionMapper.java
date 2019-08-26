package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

@Component
public class CloudbreakServerExceptionMapper extends BaseExceptionMapper<CloudbreakServiceException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<CloudbreakServiceException> getExceptionType() {
        return CloudbreakServiceException.class;
    }
}
