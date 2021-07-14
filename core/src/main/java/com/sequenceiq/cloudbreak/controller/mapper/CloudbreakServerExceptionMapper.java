package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class CloudbreakServerExceptionMapper extends BaseExceptionMapper<CloudbreakServiceException> {

    @Override
    public Status getResponseStatus(CloudbreakServiceException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<CloudbreakServiceException> getExceptionType() {
        return CloudbreakServiceException.class;
    }
}
