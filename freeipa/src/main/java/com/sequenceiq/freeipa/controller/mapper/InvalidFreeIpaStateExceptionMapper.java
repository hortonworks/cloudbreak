package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.freeipa.client.InvalidFreeIpaStateException;

@Component
public class InvalidFreeIpaStateExceptionMapper extends BaseExceptionMapper<InvalidFreeIpaStateException> {

    @Override
    protected Object getEntity(InvalidFreeIpaStateException exception) {
        return new ExceptionResponse("Error during interaction with FreeIPA: " + exception.getMessage());
    }

    @Override
    Status getResponseStatus(InvalidFreeIpaStateException exception) {
        return Status.BAD_GATEWAY;
    }

    @Override
    Class<InvalidFreeIpaStateException> getExceptionType() {
        return InvalidFreeIpaStateException.class;
    }
}
