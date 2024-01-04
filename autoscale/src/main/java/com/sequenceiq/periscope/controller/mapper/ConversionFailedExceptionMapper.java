package com.sequenceiq.periscope.controller.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.springframework.core.convert.ConversionFailedException;

@Provider
public class ConversionFailedExceptionMapper extends BaseExceptionMapper<ConversionFailedException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
