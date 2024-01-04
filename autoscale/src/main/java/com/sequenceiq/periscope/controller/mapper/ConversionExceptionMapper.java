package com.sequenceiq.periscope.controller.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import com.sequenceiq.cloudbreak.converter.ConversionException;

@Provider
public class ConversionExceptionMapper extends BaseExceptionMapper<ConversionException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
