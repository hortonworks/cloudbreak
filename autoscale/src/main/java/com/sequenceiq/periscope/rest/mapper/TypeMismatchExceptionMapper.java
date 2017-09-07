package com.sequenceiq.periscope.rest.mapper;

import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.TypeMismatchException;

@Provider
public class TypeMismatchExceptionMapper extends BaseExceptionMapper<TypeMismatchException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }
}