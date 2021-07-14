package com.sequenceiq.environment.exception.mapper;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;

@Component
    public class GetCloudParameterExceptionMapper extends SearchCauseExceptionMapper<GetCloudParameterException> {

    @Override
    public Class<GetCloudParameterException> getExceptionType() {
        return GetCloudParameterException.class;
    }

    @Override
    protected boolean logException() {
        return false;
    }
}
