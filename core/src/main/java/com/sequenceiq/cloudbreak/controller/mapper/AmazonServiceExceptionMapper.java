package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class AmazonServiceExceptionMapper extends BaseExceptionMapper<AmazonServiceException> {

    @Override
    public Status getResponseStatus(AmazonServiceException exception) {
        return Status.fromStatusCode(exception.getStatusCode());
    }

    @Override
    public Class<AmazonServiceException> getExceptionType() {
        return AmazonServiceException.class;
    }
}
