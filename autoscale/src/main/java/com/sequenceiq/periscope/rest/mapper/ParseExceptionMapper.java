package com.sequenceiq.periscope.rest.mapper;

import java.text.ParseException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

@Provider
public class ParseExceptionMapper extends BaseExceptionMapper<ParseException> {

    @Override
    protected Object getEntity(ParseException exception) {
        return exception.getMessage();
    }

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }

}
