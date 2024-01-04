package com.sequenceiq.periscope.controller.mapper;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.Provider;

import org.springframework.beans.TypeMismatchException;

@Provider
public class TypeMismatchExceptionMapper extends BaseExceptionMapper<TypeMismatchException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}
