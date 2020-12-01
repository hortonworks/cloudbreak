package com.sequenceiq.environment.exception.mapper;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.credential.exception.CredentialVerificationException;

@Component
public class CredentialVerificationExceptionMapper extends SearchCauseExceptionMapper<CredentialVerificationException> {

    @Override
    Class<CredentialVerificationException> getExceptionType() {
        return CredentialVerificationException.class;
    }

}
