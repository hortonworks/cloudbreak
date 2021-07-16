package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.cloudbreak.template.validation.BlueprintValidationException;

@Component
public class BlueprintValidationExceptionMapper extends BaseExceptionMapper<BlueprintValidationException> {

    @Override
    public Status getResponseStatus(BlueprintValidationException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<BlueprintValidationException> getExceptionType() {
        return BlueprintValidationException.class;
    }

}
