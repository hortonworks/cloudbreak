package com.sequenceiq.environment.exception.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;

@Component
public class CredentialVerificationInnerExceptionMapper extends SearchCauseExceptionMapper<CredentialVerificationException> {

    @Override
    public StatusType getResponseStatus(CredentialVerificationException exception) {
        return Status.FORBIDDEN;
    }

    @Override
    public Class<CredentialVerificationException> getExceptionType() {
        return CredentialVerificationException.class;
    }

}
