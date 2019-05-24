package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.controller.exception.NotFoundException;

@Component
public class NotFoundExceptionMapper extends BaseExceptionMapper<NotFoundException> {

    @Override
    Status getResponseStatus() {
        return Status.NOT_FOUND;
    }

    @Override
    Class<NotFoundException> getExceptionType() {
        return NotFoundException.class;
    }
}
