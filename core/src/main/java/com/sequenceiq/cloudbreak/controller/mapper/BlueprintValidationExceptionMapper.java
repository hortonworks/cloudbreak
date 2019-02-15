package com.sequenceiq.cloudbreak.controller.mapper;

import com.sequenceiq.cloudbreak.blueprint.validation.BlueprintValidationException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response.Status;

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
