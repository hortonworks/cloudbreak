package com.sequenceiq.environment.exception.mapper;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.exception.CloudUnauthorizedException;

@Component
public class CloudUnauthorizedExceptionMapper extends SearchCauseExceptionMapper<CloudUnauthorizedException> {

    @Override
    public Class<CloudUnauthorizedException> getExceptionType() {
        return CloudUnauthorizedException.class;
    }

    @Override
    protected boolean logException() {
        return false;
    }
}
