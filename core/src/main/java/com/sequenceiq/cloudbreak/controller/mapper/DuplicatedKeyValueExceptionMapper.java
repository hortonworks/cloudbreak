package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.controller.json.ExceptionResult;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Provider
public class DuplicatedKeyValueExceptionMapper extends BaseExceptionMapper<DuplicateKeyValueException> {

    @Override
    protected Object getEntity(DuplicateKeyValueException exception) {
        return new ExceptionResult(errorMessage(exception));
    }

    @Override
    Status getResponseStatus() {
        return Status.CONFLICT;
    }

    public static String errorMessage(DuplicateKeyValueException exception) {
        return String.format("The %s name '%s' is already taken, please choose a different one",
                exception.getResourceType().toString().toLowerCase(),
                exception.getValue());
    }
}
