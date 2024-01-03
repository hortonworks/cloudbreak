package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.StatusType;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.credential.exception.CredentialVerificationException;

@Component
public class CredentialVerificationExceptionMapper extends SearchCauseExceptionMapper<CredentialVerificationException> {

    @Override
    public StatusType getResponseStatus(CredentialVerificationException exception) {
        return Status.FORBIDDEN;
    }

    @Override
    public Class<CredentialVerificationException> getExceptionType() {
        return CredentialVerificationException.class;
    }

}
