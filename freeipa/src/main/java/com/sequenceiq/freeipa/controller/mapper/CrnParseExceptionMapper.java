package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;

@Component
public class CrnParseExceptionMapper extends BaseExceptionMapper<CrnParseException> {

    @Override
    Status getResponseStatus(CrnParseException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<CrnParseException> getExceptionType() {
        return CrnParseException.class;
    }

    @Override
    protected String getErrorMessage(CrnParseException exception) {
        return getResponseStatus(exception).getReasonPhrase() + ": " + exception.getMessage();
    }
}
