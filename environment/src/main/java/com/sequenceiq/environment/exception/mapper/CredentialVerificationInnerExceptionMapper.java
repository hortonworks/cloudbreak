package com.sequenceiq.environment.exception.mapper;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialVerificationException;

@Component
public class CredentialVerificationInnerExceptionMapper extends SearchCauseExceptionMapper<CredentialVerificationException> {

    @Override
    public Class<CredentialVerificationException> getExceptionType() {
        return CredentialVerificationException.class;
    }

}
