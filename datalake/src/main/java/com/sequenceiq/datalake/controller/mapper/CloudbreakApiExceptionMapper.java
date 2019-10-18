package com.sequenceiq.datalake.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;

@Provider
@Component
public class CloudbreakApiExceptionMapper extends BaseExceptionMapper<CloudbreakApiException> {

    @Override
    Status getResponseStatus() {
        return Status.CONFLICT;
    }

    @Override
    Class<CloudbreakApiException> getExceptionType() {
        return CloudbreakApiException.class;
    }
}
