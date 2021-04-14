package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;

@Component
public class AmazonServiceExceptionMapper extends BaseExceptionMapper<AmazonServiceException> {

    @Override
    Status getResponseStatus(AmazonServiceException exception) {
        return Status.fromStatusCode(exception.getStatusCode());
    }

    @Override
    Class<AmazonServiceException> getExceptionType() {
        return AmazonServiceException.class;
    }
}
