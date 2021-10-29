package com.sequenceiq.cloudbreak.exception.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;

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
}
