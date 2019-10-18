package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.exception.UnableToDeleteClusterDefinitionException;

@Component
public class UnableToDeleteClusterDefinitionExceptionMapper extends BaseExceptionMapper<UnableToDeleteClusterDefinitionException> {

    @Override
    protected Object getEntity(UnableToDeleteClusterDefinitionException exception) {
        return new ExceptionResponse("Internal server error: " + exception.getMessage());
    }

    @Override
    Status getResponseStatus() {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    Class<UnableToDeleteClusterDefinitionException> getExceptionType() {
        return UnableToDeleteClusterDefinitionException.class;
    }

}