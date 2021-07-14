package com.sequenceiq.redbeams.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class CrnParseExceptionMapper extends BaseExceptionMapper<CrnParseException> {

    @Override
    public Status getResponseStatus(CrnParseException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<CrnParseException> getExceptionType() {
        return CrnParseException.class;
    }

    @Override
    protected String getErrorMessage(CrnParseException exception) {
        return getResponseStatus(exception).getReasonPhrase() + ": " + exception.getMessage();
    }
}
