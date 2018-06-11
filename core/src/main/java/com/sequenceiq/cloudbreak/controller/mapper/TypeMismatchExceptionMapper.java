package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.beans.TypeMismatchException;
import org.springframework.stereotype.Component;

@Component
public class TypeMismatchExceptionMapper extends BaseExceptionMapper<TypeMismatchException> {

    @Override
    Status getResponseStatus() {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<TypeMismatchException> supportedType() {
        return TypeMismatchException.class;
    }
}