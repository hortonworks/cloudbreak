package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;

@Component
public class FreeIpaClientExceptionWrapperMapper extends BaseExceptionMapper<FreeIpaClientExceptionWrapper> {

    @Override
    protected Object getEntity(FreeIpaClientExceptionWrapper exception) {
        return new ExceptionResponse("Error during interaction with FreeIPA: " + exception.getMessage());
    }

    @Override
    Status getResponseStatus() {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    Class<FreeIpaClientExceptionWrapper> getExceptionType() {
        return FreeIpaClientExceptionWrapper.class;
    }
}
