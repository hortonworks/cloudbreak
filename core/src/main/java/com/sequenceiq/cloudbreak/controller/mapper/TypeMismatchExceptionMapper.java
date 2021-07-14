package com.sequenceiq.cloudbreak.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.beans.TypeMismatchException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;

@Component
public class TypeMismatchExceptionMapper extends BaseExceptionMapper<TypeMismatchException> {

    @Override
    public Status getResponseStatus(TypeMismatchException exception) {
        return Status.BAD_REQUEST;
    }

    @Override
    public Class<TypeMismatchException> getExceptionType() {
        return TypeMismatchException.class;
    }
}