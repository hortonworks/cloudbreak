package com.sequenceiq.periscope.controller.mapper;

import java.text.ParseException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import com.sequenceiq.periscope.api.model.ExceptionResult;

@Provider
public class ParseExceptionMapper extends BaseExceptionMapper<ParseException> {

    @Override
    protected Object getEntity(ParseException exception) {
        return new ExceptionResult(exception.getMessage());
    }

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

}
