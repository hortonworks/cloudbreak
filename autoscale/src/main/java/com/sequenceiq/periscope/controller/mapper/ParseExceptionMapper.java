package com.sequenceiq.periscope.controller.mapper;

import java.text.ParseException;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

@Provider
public class ParseExceptionMapper extends BaseExceptionMapper<ParseException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

}
