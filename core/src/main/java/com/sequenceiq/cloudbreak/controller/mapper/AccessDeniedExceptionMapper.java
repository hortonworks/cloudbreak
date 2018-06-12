package com.sequenceiq.cloudbreak.controller.mapper;

import java.nio.file.AccessDeniedException;

import javax.ws.rs.core.Response.Status;

import org.springframework.stereotype.Component;

@Component
public class AccessDeniedExceptionMapper extends BaseExceptionMapper<AccessDeniedException> {

    @Override
    Status getResponseStatus() {
        return Status.FORBIDDEN;
    }

    @Override
    public Class<AccessDeniedException> supportedType() {
        return AccessDeniedException.class;
    }
}
