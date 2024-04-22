package com.sequenceiq.environment.exception.mapper;

import jakarta.ws.rs.core.Response;

import org.springframework.stereotype.Component;
import org.springframework.vault.VaultException;

@Component
public class VaultExceptionMapper extends EnvironmentBaseExceptionMapper<VaultException> {

    @Override
    public Response.StatusType getResponseStatus(VaultException exception) {
        return Response.Status.INTERNAL_SERVER_ERROR;
    }

    @Override
    public Class<VaultException> getExceptionType() {
        return VaultException.class;
    }

}
