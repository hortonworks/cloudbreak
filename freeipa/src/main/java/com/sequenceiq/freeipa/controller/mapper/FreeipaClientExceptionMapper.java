package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.freeipa.client.FreeIpaClientException;

@Component
public class FreeipaClientExceptionMapper extends BaseExceptionMapper<FreeIpaClientException> {

    @Override
    protected String getErrorMessage(FreeIpaClientException exception) {
        return "Error during interaction with FreeIPA: " + exception.getMessage();
    }

    @Override
    public Status getResponseStatus(FreeIpaClientException exception) {
        return Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    public Class<FreeIpaClientException> getExceptionType() {
        return FreeIpaClientException.class;
    }
}
