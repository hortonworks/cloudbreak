package com.sequenceiq.freeipa.controller.mapper;

import jakarta.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.freeipa.client.FreeIpaClientBuildException;

@Component
public class FreeipaClientBuildExceptionMapper extends BaseExceptionMapper<FreeIpaClientBuildException> {

    @Override
    protected String getErrorMessage(FreeIpaClientBuildException exception) {
        return "Error during creating FreeIPA client: " + exception.getMessage();
    }

    @Override
    public Status getResponseStatus(FreeIpaClientBuildException exception) {
        return Status.BAD_GATEWAY;
    }

    @Override
    public Class<FreeIpaClientBuildException> getExceptionType() {
        return FreeIpaClientBuildException.class;
    }
}
