package com.sequenceiq.freeipa.controller.mapper;

import com.sequenceiq.freeipa.controller.exception.UnsupportedException;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response.Status;

@Component
public class UnsupportedExceptionMapper extends BaseExceptionMapper<UnsupportedException> {

    @Override
    Status getResponseStatus() {
        return Status.FORBIDDEN;
    }

    @Override
    Class<UnsupportedException> getExceptionType() {
        return UnsupportedException.class;
    }
}
