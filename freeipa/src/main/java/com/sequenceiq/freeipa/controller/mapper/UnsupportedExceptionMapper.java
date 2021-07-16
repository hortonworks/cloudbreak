package com.sequenceiq.freeipa.controller.mapper;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.exception.mapper.BaseExceptionMapper;
import com.sequenceiq.freeipa.controller.exception.UnsupportedException;

@Component
public class UnsupportedExceptionMapper extends BaseExceptionMapper<UnsupportedException> {

    @Override
    public Status getResponseStatus(UnsupportedException exception) {
        return Status.FORBIDDEN;
    }

    @Override
    public Class<UnsupportedException> getExceptionType() {
        return UnsupportedException.class;
    }
}
