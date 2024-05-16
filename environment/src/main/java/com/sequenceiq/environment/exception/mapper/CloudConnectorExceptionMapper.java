package com.sequenceiq.environment.exception.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Component
public class CloudConnectorExceptionMapper extends SearchCauseExceptionMapper<CloudConnectorException> {

    @Override
    public StatusType getResponseStatus(CloudConnectorException exception) {
        return Status.FORBIDDEN;
    }

    @Override
    public Class<CloudConnectorException> getExceptionType() {
        return CloudConnectorException.class;
    }

}
