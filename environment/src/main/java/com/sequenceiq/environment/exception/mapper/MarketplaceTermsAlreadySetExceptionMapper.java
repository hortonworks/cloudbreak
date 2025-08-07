package com.sequenceiq.environment.exception.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.exception.TermsAlreadySetException;

@Provider
@Component
public class MarketplaceTermsAlreadySetExceptionMapper extends EnvironmentBaseExceptionMapper<TermsAlreadySetException> {

    @Override
    public Status getResponseStatus(TermsAlreadySetException exception) {
        return Status.CONFLICT;
    }

    @Override
    public Class<TermsAlreadySetException> getExceptionType() {
        return TermsAlreadySetException.class;
    }

}
