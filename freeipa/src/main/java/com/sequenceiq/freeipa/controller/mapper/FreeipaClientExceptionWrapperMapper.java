package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.ExceptionResponse;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;

@Component
public class FreeipaClientExceptionWrapperMapper extends BaseExceptionMapper<FreeIpaClientExceptionWrapper> {

    @Override
    protected Object getEntity(FreeIpaClientExceptionWrapper exception) {
        return new ExceptionResponse("Error during interaction with FreeIPA: " + exception.getWrappedException().getMessage());
    }

    @Override
    public Status getResponseStatus(FreeIpaClientExceptionWrapper exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    public Class<FreeIpaClientExceptionWrapper> getExceptionType() {
        return FreeIpaClientExceptionWrapper.class;
    }
}
