package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;

@Component
public class FreeipaClientExceptionMapper extends BaseExceptionMapper<FreeIpaClientException> {

    @Override
    protected Object getEntity(FreeIpaClientException exception) {
        return new ExceptionResponse("Error during interaction with FreeIPA: " + exception.getMessage());
    }

    @Override
    Status getResponseStatus(FreeIpaClientException exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    Class<FreeIpaClientException> getExceptionType() {
        return FreeIpaClientException.class;
    }
}
