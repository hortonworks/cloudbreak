package com.sequenceiq.environment.exception.mapper;

import javax.ws.rs.core.Response;

import com.sequenceiq.environment.credential.exception.CredentialVerificationException;

public class CredentialVerificationExceptionMapper extends BaseExceptionMapper<CredentialVerificationException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    Class<CredentialVerificationException> getExceptionType() {
        return CredentialVerificationException.class;
    }

}
