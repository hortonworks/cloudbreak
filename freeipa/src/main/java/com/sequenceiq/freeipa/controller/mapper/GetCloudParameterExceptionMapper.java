package com.sequenceiq.freeipa.controller.mapper;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;

import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.service.GetCloudParameterException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class GetCloudParameterExceptionMapper extends BaseExceptionMapper<GetCloudParameterException> {

    @Override
    public Status getResponseStatus(GetCloudParameterException exception) {
        return BAD_REQUEST;
    }

    @Override
    public Class<GetCloudParameterException> getExceptionType() {
        return GetCloudParameterException.class;
    }

    @Override
    protected boolean logException(GetCloudParameterException exception) {
        return false;
    }
}
