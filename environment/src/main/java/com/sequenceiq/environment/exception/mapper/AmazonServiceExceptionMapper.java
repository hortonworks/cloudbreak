package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;

@Component
public class AmazonServiceExceptionMapper extends EnvironmentBaseExceptionMapper<AmazonServiceException> {

    @Override
    public Status getResponseStatus(AmazonServiceException exception) {
        return Status.fromStatusCode(exception.getStatusCode());
    }

    @Override
    public Class<AmazonServiceException> getExceptionType() {
        return AmazonServiceException.class;
    }
}
