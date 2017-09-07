package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.springframework.core.convert.ConversionFailedException;

@Provider
public class ConversionFailedExceptionMapper extends BaseExceptionMapper<ConversionFailedException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}