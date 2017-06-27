package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.TypeMismatchException;

@Provider
public class TypeMismatchExceptionMapper extends BaseExceptionMapper<TypeMismatchException> {

    @Override
    Response.Status getResponseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}