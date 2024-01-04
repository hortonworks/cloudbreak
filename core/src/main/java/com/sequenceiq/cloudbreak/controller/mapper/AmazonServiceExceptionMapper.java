package com.sequenceiq.cloudbreak.controller.mapper;

import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

import software.amazon.awssdk.awscore.exception.AwsServiceException;

@Component
public class AmazonServiceExceptionMapper extends BaseExceptionMapper<AwsServiceException> {

    @Override
    public Status getResponseStatus(AwsServiceException exception) {
        return Status.fromStatusCode(exception.awsErrorDetails().sdkHttpResponse().statusCode());
    }

    @Override
    public Class<AwsServiceException> getExceptionType() {
        return AwsServiceException.class;
    }
}
