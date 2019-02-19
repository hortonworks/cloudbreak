package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.validation.ClusterDefinitionValidationException;

@Component
public class ClusterDefinitionValidationExceptionMapper extends BaseExceptionMapper<ClusterDefinitionValidationException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<ClusterDefinitionValidationException> getExceptionType() {
        return ClusterDefinitionValidationException.class;
    }

}
