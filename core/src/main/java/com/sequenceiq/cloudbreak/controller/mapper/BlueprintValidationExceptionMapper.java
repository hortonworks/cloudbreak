package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.template.validation.ClusterDefinitionValidationException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response.Status;

@Component
public class BlueprintValidationExceptionMapper extends BaseExceptionMapper<ClusterDefinitionValidationException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<ClusterDefinitionValidationException> getExceptionType() {
        return ClusterDefinitionValidationException.class;
    }

}
