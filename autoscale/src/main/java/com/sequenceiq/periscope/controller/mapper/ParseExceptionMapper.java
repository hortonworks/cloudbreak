package com.sequenceiq.periscope.controller.mapper;

import java.text.ParseException;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ParseExceptionMapper extends BaseExceptionMapper<ParseException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

}
