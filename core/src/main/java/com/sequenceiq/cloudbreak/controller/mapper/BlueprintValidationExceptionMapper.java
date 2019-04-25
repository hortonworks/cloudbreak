package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.template.validation.BlueprintValidationException;

@Component
public class BlueprintValidationExceptionMapper extends BaseExceptionMapper<BlueprintValidationException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<BlueprintValidationException> getExceptionType() {
        return BlueprintValidationException.class;
    }

}
