package com.sequenceiq.datalake.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Provider
@Component
public class CloudbreakApiExceptionMapper extends BaseExceptionMapper<CloudbreakApiException> {

    @Override
    public Status getResponseStatus(CloudbreakApiException exception) {
        return Status.CONFLICT;
    }

    @Override
    public Class<CloudbreakApiException> getExceptionType() {
        return CloudbreakApiException.class;
    }
}
