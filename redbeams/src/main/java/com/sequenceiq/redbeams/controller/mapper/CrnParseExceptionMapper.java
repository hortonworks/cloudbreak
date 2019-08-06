package com.sequenceiq.redbeams.controller.mapper;

import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class CrnParseExceptionMapper extends BaseExceptionMapper<CrnParseException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    Class<CrnParseException> getExceptionType() {
        return CrnParseException.class;
    }

    @Override
    protected String getErrorMessage(CrnParseException exception) {
        return getResponseStatus().getReasonPhrase() + ": " + exception.getMessage();
    }
}
